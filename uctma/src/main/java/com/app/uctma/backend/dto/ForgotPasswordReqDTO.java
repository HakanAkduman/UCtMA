package com.app.uctma.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordReqDTO(
        @NotBlank @Email String email
) {
}
