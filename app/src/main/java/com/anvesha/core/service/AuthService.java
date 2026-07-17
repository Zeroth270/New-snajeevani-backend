package com.anvesha.core.service;

import com.anvesha.core.dto.auth.AuthResponse;
import com.anvesha.core.dto.auth.LoginRequest;
import com.anvesha.core.dto.auth.RegisterRequest;
import com.anvesha.core.entity.AppUser;
import com.anvesha.core.entity.Institution;
import com.anvesha.core.exception.BadRequestException;
import com.anvesha.core.exception.ResourceNotFoundException;
import com.anvesha.core.repository.AppUserRepository;
import com.anvesha.core.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BadRequestException("Email already in use: " + req.email());
        }

        Institution institution = institutionRepository.findById(req.institutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution", req.institutionId()));

        AppUser user = AppUser.builder()
                .name(req.name())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(req.role())
                .institution(institution)
                .build();

        userRepository.save(user);
        log.info("Registered new user: {} with role {}", req.email(), req.role());

        // Return email as the bypass token
        return AuthResponse.of(user.getEmail(), 31536000000L,
                user.getEmail(), user.getRole(), institution.getId());
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        AppUser user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.email()));

        // Return email as the bypass token
        return AuthResponse.of(user.getEmail(), 31536000000L,
                user.getEmail(), user.getRole(),
                user.getInstitution() != null ? user.getInstitution().getId() : null);
    }
}
