package com.anvesha.core.controller;

import com.anvesha.core.dto.institution.InstitutionResponse;
import com.anvesha.core.repository.InstitutionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/institutions")
@RequiredArgsConstructor
@Tag(name = "Institutions", description = "Management and listing of institutions")
public class InstitutionController {

    private final InstitutionRepository institutionRepository;

    @GetMapping
    @Operation(summary = "Get all registered institutions", description = "Returns a list of all institutions for the registration dropdown")
    public List<InstitutionResponse> getAll() {
        return institutionRepository.findAll().stream()
                .map(InstitutionResponse::from)
                .collect(Collectors.toList());
    }
}
