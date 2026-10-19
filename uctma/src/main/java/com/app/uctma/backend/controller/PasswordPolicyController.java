package com.app.uctma.backend.controller;

import com.app.uctma.backend.dto.PasswordPolicyDTO;
import com.app.uctma.backend.dto.PasswordPolicyUpdateReqDTO;
import com.app.uctma.backend.model.ApiResponse;
import com.app.uctma.backend.service.PasswordPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/uctma/api/auth/password-policies")
@RequiredArgsConstructor
public class PasswordPolicyController {

    private final PasswordPolicyService passwordPolicyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PasswordPolicyDTO>>> getPolicies() {
        return ResponseEntity.ok(ApiResponse.success(passwordPolicyService.getPolicies()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<List<PasswordPolicyDTO>>> updatePolicies(
            @Valid @RequestBody PasswordPolicyUpdateReqDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.success(passwordPolicyService.updatePolicies(request)));
    }
}
