package com.anvesha.core.dto.molecule;

import com.anvesha.core.dto.scan.NoveltyScanResponse;
import com.anvesha.core.entity.Molecule;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MoleculeResponse(
        Long id,
        Long paperId,
        String extractedNameRaw,
        String iupacName,
        String smiles,
        BigDecimal extractionConfidence,
        OffsetDateTime createdAt,
        NoveltyScanResponse latestScan
) {
    public static MoleculeResponse from(Molecule m, NoveltyScanResponse latestScan) {
        return new MoleculeResponse(
                m.getId(),
                m.getPaper() != null ? m.getPaper().getId() : null,
                m.getExtractedNameRaw(),
                m.getIupacName(),
                m.getSmiles(),
                m.getExtractionConfidence(),
                m.getCreatedAt(),
                latestScan
        );
    }
}
