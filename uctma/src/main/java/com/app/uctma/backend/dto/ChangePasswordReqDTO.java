package com.app.uctma.backend.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangePasswordReqDTO {
    @NotEmpty(message = "Current password can not be empty")
    String currentPassword;

    @NotEmpty(message = "New password can not be empty")
    String newPassword;

    @NotEmpty(message = "Confirmation of new password can not be empty")
    String confirmNewPassword;


}
