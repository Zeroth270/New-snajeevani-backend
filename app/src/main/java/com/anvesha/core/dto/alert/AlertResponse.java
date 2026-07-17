package com.anvesha.core.dto.alert;

import com.anvesha.core.entity.Alert;

import java.time.OffsetDateTime;

public record AlertResponse(
        Long id,
        String alertType,
        String message,
        boolean isRead,
        Long disclosureWindowId,
        OffsetDateTime createdAt
) {
    public static AlertResponse from(Alert a) {
        return new AlertResponse(
                a.getId(), a.getAlertType(), a.getMessage(), a.isRead(),
                a.getDisclosureWindow() != null ? a.getDisclosureWindow().getId() : null,
                a.getCreatedAt()
        );
    }
}
