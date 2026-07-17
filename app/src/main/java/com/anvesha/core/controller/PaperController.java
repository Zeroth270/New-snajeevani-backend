package com.anvesha.core.controller;

import com.anvesha.core.dto.paper.*;
import com.anvesha.core.service.PaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
@Tag(name = "Papers", description = "Upload and query research papers")
@SecurityRequirement(name = "bearerAuth")
public class PaperController {

    private final PaperService paperService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Upload a paper (PDF or raw text). Returns 202 — processing is async.")
    public PaperResponse upload(
            @RequestParam String title,
            @RequestParam(required = false) String authors,
            @RequestParam String sourceType,
            @RequestParam(required = false) String publicationDate,
            @RequestParam(required = false) String rawText,
            @RequestParam(required = false) MultipartFile file,
            @AuthenticationPrincipal UserDetails principal) {

        LocalDate pubDate = null;
        if (publicationDate != null && !publicationDate.isBlank()) {
            try {
                pubDate = LocalDate.parse(publicationDate);
            } catch (Exception e) {
                throw new com.anvesha.core.exception.BadRequestException("Invalid publicationDate format. Please use YYYY-MM-DD.");
            }
        }

        PaperUploadRequest req = new PaperUploadRequest(
                title, authors, sourceType,
                pubDate,
                rawText, file);

        return paperService.upload(req, principal);
    }

    @GetMapping
    @Operation(summary = "List papers (paginated, filterable by status). Scoped by caller's role.")
    public Page<PaperResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails principal) {
        return paperService.list(principal, status, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full paper detail including molecules and their novelty scans.")
    public PaperDetailResponse getById(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetails principal) {
        return paperService.getById(id, principal);
    }
}
