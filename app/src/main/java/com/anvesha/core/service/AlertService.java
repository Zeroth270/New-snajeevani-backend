package com.anvesha.core.service;

import com.anvesha.core.dto.alert.AlertResponse;
import com.anvesha.core.entity.Alert;
import com.anvesha.core.entity.AppUser;
import com.anvesha.core.exception.ResourceNotFoundException;
import com.anvesha.core.repository.AlertRepository;
import com.anvesha.core.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AppUserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AlertResponse> listForCurrentUser(UserDetails principal, boolean unreadOnly,
                                                   int page, int size) {
        AppUser user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Alert> alerts = unreadOnly
                ? alertRepository.findByRecipientUserIdAndIsRead(user.getId(), false, pageable)
                : alertRepository.findByRecipientUserId(user.getId(), pageable);

        return alerts.map(AlertResponse::from);
    }

    @Transactional
    public AlertResponse markRead(Long alertId, UserDetails principal) {
        AppUser user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));

        if (!alert.getRecipientUser().getId().equals(user.getId())) {
            throw new com.anvesha.core.exception.BadRequestException("Alert does not belong to current user");
        }

        alert.setRead(true);
        alert = alertRepository.save(alert);
        return AlertResponse.from(alert);
    }
}
