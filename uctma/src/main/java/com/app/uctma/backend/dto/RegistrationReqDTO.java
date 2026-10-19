package com.app.uctma.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistrationReqDTO(
        @Email @NotBlank String email,
        @NotBlank String password
) {
}
