package com.anvesha.core.dto.filing;

import com.anvesha.core.entity.PatentFiling;

import java.time.LocalDate;

public record PatentFilingResponse(
        Long id,
        Long moleculeId,
        Long filedById,
        LocalDate filingDate,
        String patentOffice,
        String applicationNumber,
        String status
) {
    public static PatentFilingResponse from(PatentFiling pf) {
        return new PatentFilingResponse(
                pf.getId(),
                pf.getMolecule() != null ? pf.getMolecule().getId() : null,
                pf.getFiledBy() != null ? pf.getFiledBy().getId() : null,
                pf.getFilingDate(),
                pf.getPatentOffice(),
                pf.getApplicationNumber(),
                pf.getStatus()
        );
    }
}
