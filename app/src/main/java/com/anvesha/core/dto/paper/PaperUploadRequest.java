package com.anvesha.core.dto.paper;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public record PaperUploadRequest(
        @NotBlank String title,
        String authors,
        @NotBlank @Pattern(regexp = "PREPRINT|JOURNAL|THESIS|CONFERENCE",
                message = "sourceType must be one of: PREPRINT, JOURNAL, THESIS, CONFERENCE")
        String sourceType,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate publicationDate,
        /** Raw text content — supply either this or the file */
        String rawText,
        /** PDF upload — optional */
        MultipartFile file
) {}
