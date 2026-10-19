package com.app.uctma.backend.dto;


import com.app.uctma.backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class LoginResDTO {

    @Email
    private String email;
    private String token;
    private String tokenType;
    private boolean newPasswordRequired;
    private Role role;
    private boolean passwordResetSession;



}
