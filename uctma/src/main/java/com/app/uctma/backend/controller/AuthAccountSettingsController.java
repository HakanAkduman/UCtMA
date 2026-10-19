package com.app.uctma.backend.controller;

import com.app.uctma.backend.dto.AuthAccountSettingDTO;
import com.app.uctma.backend.dto.AuthAccountSettingsUpdateReqDTO;
import com.app.uctma.backend.dto.AuthPublicSettingsDTO;
import com.app.uctma.backend.model.ApiResponse;
import com.app.uctma.backend.service.AuthAccountSettingsService;
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
@RequestMapping("/uctma/api/auth")
@RequiredArgsConstructor
public class AuthAccountSettingsController {

    private final AuthAccountSettingsService settingsService;

    @GetMapping("/public-settings")
    public ResponseEntity<ApiResponse<AuthPublicSettingsDTO>> getPublicSettings() {
        return ResponseEntity.ok(ApiResponse.success(settingsService.getPublicSettings()));
    }

    @GetMapping("/account-settings")
    public ResponseEntity<ApiResponse<List<AuthAccountSettingDTO>>> getAccountSettings() {
        return ResponseEntity.ok(ApiResponse.success(settingsService.getSettings()));
    }

    @PutMapping("/account-settings")
    public ResponseEntity<ApiResponse<List<AuthAccountSettingDTO>>> updateAccountSettings(
            @Valid @RequestBody AuthAccountSettingsUpdateReqDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.updateSettings(request)));
    }
}
