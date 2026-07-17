package com.anvesha.core.controller;

import com.anvesha.core.dto.window.DisclosureWindowResponse;
import com.anvesha.core.service.DisclosureWindowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disclosure-windows")
@RequiredArgsConstructor
@Tag(name = "Disclosure Windows", description = "Patent grace-period tracking")
@SecurityRequirement(name = "bearerAuth")
public class DisclosureWindowController {

    private final DisclosureWindowService service;

    @GetMapping
    @Operation(summary = "List disclosure windows. Use ?status=CLOSING_SOON for dashboard feed.")
    public Page<DisclosureWindowResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(status, institutionId, page, size);
    }
}
