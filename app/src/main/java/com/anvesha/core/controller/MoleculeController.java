package com.anvesha.core.controller;

import com.anvesha.core.dto.filing.PatentFilingRequest;
import com.anvesha.core.dto.filing.PatentFilingResponse;
import com.anvesha.core.dto.molecule.MoleculeResponse;
import com.anvesha.core.dto.window.CreateDisclosureWindowRequest;
import com.anvesha.core.dto.window.DisclosureWindowResponse;
import com.anvesha.core.service.DisclosureWindowService;
import com.anvesha.core.service.MoleculeService;
import com.anvesha.core.service.PatentFilingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/molecules")
@RequiredArgsConstructor
@Tag(name = "Molecules", description = "Molecule detail, disclosure windows, and patent filings")
@SecurityRequirement(name = "bearerAuth")
public class MoleculeController {

    private final MoleculeService moleculeService;
    private final DisclosureWindowService disclosureWindowService;
    private final PatentFilingService patentFilingService;

    @GetMapping("/{id}")
    @Operation(summary = "Get a molecule and its latest novelty scan")
    public MoleculeResponse getById(@PathVariable Long id) {
        return moleculeService.getById(id);
    }

    @PostMapping("/{id}/disclosure-window")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a disclosure window for the paper containing this molecule")
    public DisclosureWindowResponse createDisclosureWindow(
            @PathVariable Long id,
            @Valid @RequestBody CreateDisclosureWindowRequest req) {
        // We use the molecule's paper as the anchor — look it up via molecule
        MoleculeResponse mol = moleculeService.getById(id);
        return disclosureWindowService.createForMolecule(mol.paperId(), req);
    }

    @PostMapping("/{id}/patent-filing")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a patent filing outcome for a molecule")
    public PatentFilingResponse recordFiling(
            @PathVariable Long id,
            @Valid @RequestBody PatentFilingRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return patentFilingService.record(id, req, principal);
    }
}
