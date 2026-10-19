package com.app.uctma.backend.dto;

public record AuthPublicSettingsDTO(
        boolean registrationEnabled,
        boolean emailVerificationRequired
) {
}
