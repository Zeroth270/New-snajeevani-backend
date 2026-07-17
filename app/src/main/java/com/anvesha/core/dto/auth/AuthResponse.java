package com.anvesha.core.dto.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String email,
        String role,
        Long institutionId
) {
    public static AuthResponse of(String token, long expiresInMs, String email, String role, Long institutionId) {
        return new AuthResponse(token, "Bearer", expiresInMs, email, role, institutionId);
    }
}
