package com.anvesha.core.dto.paper;

import com.anvesha.core.entity.Paper;
import com.anvesha.core.entity.NoveltyScan;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PaperResponse(
        Long id,
        String title,
        String authors,
        String sourceType,
        LocalDate publicationDate,
        String status,
        Long institutionId,
        Long uploadedById,
        OffsetDateTime createdAt,
        int moleculeCount,
        int novelMoleculeCount
) {
    public static PaperResponse from(Paper p) {
        int molCount = p.getMolecules() != null ? p.getMolecules().size() : 0;
        int novelCount = p.getMolecules() != null 
                ? (int) p.getMolecules().stream().filter(m -> 
                    m.getNoveltyScans() != null && m.getNoveltyScans().stream()
                            .max(java.util.Comparator.comparing(NoveltyScan::getScannedAt))
                            .map(NoveltyScan::isNovel)
                            .orElse(false)
                ).count() 
                : 0;

        return new PaperResponse(
                p.getId(), p.getTitle(), p.getAuthors(), p.getSourceType(),
                p.getPublicationDate(), p.getStatus(),
                p.getInstitution() != null ? p.getInstitution().getId() : null,
                p.getUploadedBy() != null ? p.getUploadedBy().getId() : null,
                p.getCreatedAt(),
                molCount,
                novelCount
        );
    }
}
