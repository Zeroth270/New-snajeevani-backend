package com.anvesha.core.dto.window;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateDisclosureWindowRequest(
        @NotNull LocalDate disclosureDate,
        Integer gracePeriodDays   // nullable; defaults to 365 if not supplied
) {}
