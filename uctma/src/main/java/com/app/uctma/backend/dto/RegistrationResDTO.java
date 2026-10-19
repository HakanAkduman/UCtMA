package com.app.uctma.backend.dto;

public record RegistrationResDTO(
        String message,
        boolean emailVerificationRequired
) {
}
