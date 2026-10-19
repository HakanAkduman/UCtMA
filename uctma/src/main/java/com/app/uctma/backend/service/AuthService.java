package com.app.uctma.backend.service;

import com.app.uctma.backend.dto.*;
import com.app.uctma.backend.model.ApiResponse;
import com.app.uctma.backend.model.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;

public interface AuthService {
    public String invite(InviteReqDTO inviteReqDTO);

    public String delete(String email);

    public List<UserListResDTO> listOfUsers();

    public UserListResDTO updateUserEnabled(String email, boolean enabled);

    public LoginResDTO login( LoginReqDTO loginReqDTO);

    public String forgotPassword(ForgotPasswordReqDTO forgotPasswordReqDTO);

    public String changePassword(ChangePasswordReqDTO changePasswordReqDTO, String passwordResetSessionId);
}
