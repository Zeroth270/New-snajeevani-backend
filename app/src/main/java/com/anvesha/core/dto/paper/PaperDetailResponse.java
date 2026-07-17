package com.anvesha.core.dto.paper;

import com.anvesha.core.dto.molecule.MoleculeResponse;
import com.anvesha.core.entity.Paper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record PaperDetailResponse(
        Long id,
        String title,
        String authors,
        String sourceType,
        LocalDate publicationDate,
        String status,
        Long institutionId,
        Long uploadedById,
        OffsetDateTime createdAt,
        List<MoleculeResponse> molecules
) {
    public static PaperDetailResponse from(Paper p, List<MoleculeResponse> molecules) {
        return new PaperDetailResponse(
                p.getId(), p.getTitle(), p.getAuthors(), p.getSourceType(),
                p.getPublicationDate(), p.getStatus(),
                p.getInstitution() != null ? p.getInstitution().getId() : null,
                p.getUploadedBy() != null ? p.getUploadedBy().getId() : null,
                p.getCreatedAt(),
                molecules
        );
    }
}
