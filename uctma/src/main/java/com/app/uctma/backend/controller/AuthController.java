package com.app.uctma.backend.controller;

import com.app.uctma.backend.dto.ChangePasswordReqDTO;
import com.app.uctma.backend.dto.LoginReqDTO;
import com.app.uctma.backend.dto.LoginResDTO;
import com.app.uctma.backend.dto.RegisterReqDTO;
import com.app.uctma.backend.model.ApiResponse;
import com.app.uctma.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/uctma/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 1. Yeni Kullanıcı Kaydı (Sadece Admin yetkisi olanlar erişebilir - SecurityConfig'de ayarladık)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterReqDTO registerReqDTO) {
        ApiResponse<String> response = ApiResponse.success(authService.register(registerReqDTO), HttpStatus.CREATED);
        return ResponseEntity.status(response.code()).body(response);
    }

    // 2. Kullanıcı Girişi (Herkese açık)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResDTO>> login(@Valid @RequestBody LoginReqDTO loginReqDTO) {
        LoginResDTO loginResDTO = authService.login(loginReqDTO);
        return ResponseEntity.ok(ApiResponse.success(loginResDTO));
    }

    // 3. Şifre Değiştirme (Giriş yapmış / Token sahibi kullanıcılar erişebilir)
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordReqDTO changePasswordReqDTO) {
        authService.changePassword(changePasswordReqDTO);
        return ResponseEntity.ok(ApiResponse.success("Şifreniz başarıyla değiştirildi."));
    }
}
