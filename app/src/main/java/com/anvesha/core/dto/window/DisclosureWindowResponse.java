package com.anvesha.core.dto.window;

import com.anvesha.core.entity.DisclosureWindow;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DisclosureWindowResponse(
        Long id,
        Long paperId,
        LocalDate disclosureDate,
        int gracePeriodDays,
        LocalDate deadlineDate,
        boolean qualifiesForSection31,
        String status,
        OffsetDateTime createdAt
) {
    public static DisclosureWindowResponse from(DisclosureWindow dw) {
        return new DisclosureWindowResponse(
                dw.getId(),
                dw.getPaper() != null ? dw.getPaper().getId() : null,
                dw.getDisclosureDate(),
                dw.getGracePeriodDays(),
                dw.getDeadlineDate(),
                dw.isQualifiesForSection31(),
                dw.getStatus(),
                dw.getCreatedAt()
        );
    }
}
