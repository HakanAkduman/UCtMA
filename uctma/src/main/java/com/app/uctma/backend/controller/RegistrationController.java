package com.app.uctma.backend.controller;

import com.app.uctma.backend.dto.RegistrationReqDTO;
import com.app.uctma.backend.dto.RegistrationResDTO;
import com.app.uctma.backend.dto.VerifyEmailReqDTO;
import com.app.uctma.backend.model.ApiResponse;
import com.app.uctma.backend.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/uctma/api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegistrationResDTO>> register(
            @Valid @RequestBody RegistrationReqDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(registrationService.register(request), HttpStatus.CREATED));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@Valid @RequestBody VerifyEmailReqDTO request) {
        return ResponseEntity.ok(ApiResponse.success(registrationService.verifyEmail(request)));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<String>> resendVerification(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(registrationService.resendVerificationCode(email)));
    }
}
