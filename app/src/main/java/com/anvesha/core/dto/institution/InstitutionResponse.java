package com.anvesha.core.dto.institution;

import com.anvesha.core.entity.Institution;

public record InstitutionResponse(
        Long id,
        String name,
        String type,
        String ttoContactEmail
) {
    public static InstitutionResponse from(Institution inst) {
        return new InstitutionResponse(
                inst.getId(),
                inst.getName(),
                inst.getType(),
                inst.getTtoContactEmail()
        );
    }
}
