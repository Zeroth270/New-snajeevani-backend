package com.anvesha.core.security;

import com.anvesha.core.entity.AppUser;
import com.anvesha.core.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class BypassAuthenticationFilter extends OncePerRequestFilter {

    private final AppUserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String email = extractEmail(request);

        // Fallback: If no email is provided in headers/parameters, auto-select the first user in the database.
        // This allows completely header-less requests to work out of the box in development.
        if (email == null) {
            email = userRepository.findAll().stream()
                    .findFirst()
                    .map(AppUser::getEmail)
                    .orElse(null);
        }

        if (email != null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                log.debug("Bypass authentication failed for email '{}': {}", email, ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractEmail(HttpServletRequest request) {
        // 1. Try X-User-Email custom header
        String email = request.getHeader("X-User-Email");
        if (StringUtils.hasText(email)) {
            return email.trim();
        }

        // 2. Try Authorization Bearer header (often contains the token/email returned from login)
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        // 3. Try query parameter ?email=...
        String emailParam = request.getParameter("email");
        if (StringUtils.hasText(emailParam)) {
            return emailParam.trim();
        }

        return null;
    }
}
