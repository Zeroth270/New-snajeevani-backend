package com.anvesha.core.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull Long institutionId,
        @NotBlank @Pattern(regexp = "RESEARCHER|TTO_OFFICER", message = "Role must be RESEARCHER or TTO_OFFICER")
        String role
) {}
