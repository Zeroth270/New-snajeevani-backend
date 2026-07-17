package com.anvesha.core.dto.error;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        String path,
        OffsetDateTime timestamp,
        List<FieldViolation> fieldErrors
) {
    public record FieldViolation(String field, String message) {}

    /** Convenience factory for simple errors without field violations */
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(status, error, message, path, OffsetDateTime.now(), List.of());
    }
}
