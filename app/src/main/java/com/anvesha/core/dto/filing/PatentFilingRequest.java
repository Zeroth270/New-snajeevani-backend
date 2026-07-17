package com.anvesha.core.dto.filing;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record PatentFilingRequest(
        @NotBlank String patentOffice,
        LocalDate filingDate,
        String applicationNumber
) {}
