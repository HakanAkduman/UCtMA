package com.app.uctma.backend.controller;

import com.app.uctma.backend.dto.*;
import com.app.uctma.backend.model.ApiResponse;
import com.app.uctma.backend.service.AuthService;
import com.app.uctma.backend.service.JwtService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/uctma/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<String>> invite(@Valid @RequestBody InviteReqDTO inviteReqDTO) {
        ApiResponse<String> response = ApiResponse.success(authService.invite(inviteReqDTO), HttpStatus.CREATED);
        return ResponseEntity.status(response.code()).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResDTO>> login(@Valid @RequestBody LoginReqDTO loginReqDTO) {
        LoginResDTO loginResDTO = authService.login(loginReqDTO);
        return ResponseEntity.ok(ApiResponse.success(loginResDTO));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordReqDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.success(authService.forgotPassword(request)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordReqDTO changePasswordReqDTO,
            @RequestHeader("Authorization") String authorization
    ) {
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : "";
        String resetSessionId = token.isEmpty() ? null : jwtService.extractPasswordResetSessionId(token);
        authService.changePassword(changePasswordReqDTO, resetSessionId);
        return ResponseEntity.ok(ApiResponse.success("Şifreniz başarıyla değiştirildi."));
    }

    @GetMapping("/list-users")
    public ResponseEntity<ApiResponse<List<UserListResDTO>>> listAllUsers(){

        return ResponseEntity.ok(ApiResponse.success(authService.listOfUsers()));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deleteUser(@RequestParam String email){
        return ResponseEntity.ok(ApiResponse.success(authService.delete(email)));
    }

    @PutMapping("/users/{email}/enabled")
    public ResponseEntity<ApiResponse<UserListResDTO>> updateUserEnabled(
            @PathVariable String email,
            @Valid @RequestBody UserEnabledUpdateReqDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.updateUserEnabled(email, request.enabled())
        ));
    }
}
