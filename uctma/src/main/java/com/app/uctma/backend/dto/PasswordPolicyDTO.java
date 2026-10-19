package com.app.uctma.backend.dto;

public record PasswordPolicyDTO(
        Long id,
        String passwordPolicyCode,
        String description,
        boolean enabled,
        int qty
) {
}
