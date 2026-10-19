package com.app.uctma.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PasswordPolicyUpdateItemDTO(
        @NotBlank String passwordPolicyCode,
        boolean enabled,
        @Min(0) @Max(512) int qty
) {
}
