package com.app.uctma.backend.dto;

import com.app.uctma.backend.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterReqDTO {
    @Email
    @NotEmpty(message = "Email can not be empty")
    private String email;

    @NotNull(message = "Role can not be empty")
    private Role role;
}
