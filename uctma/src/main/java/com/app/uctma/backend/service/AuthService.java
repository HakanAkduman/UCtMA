package com.app.uctma.backend.service;

import com.app.uctma.backend.dto.ChangePasswordReqDTO;
import com.app.uctma.backend.dto.LoginReqDTO;
import com.app.uctma.backend.dto.LoginResDTO;
import com.app.uctma.backend.dto.RegisterReqDTO;
import com.app.uctma.backend.model.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthService {
    public String register(RegisterReqDTO registerReqDTO);

    public LoginResDTO login( LoginReqDTO loginReqDTO);

    public String changePassword(ChangePasswordReqDTO changePasswordReqDTO);
}
