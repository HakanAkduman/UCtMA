package com.app.uctma.backend.dto;


import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ChangePasswordReqDTO {
    String currentPassword;

    @NotEmpty(message = "New password can not be empty")
    String newPassword;

    @NotEmpty(message = "Confirmation of new password can not be empty")
    String confirmNewPassword;


}
