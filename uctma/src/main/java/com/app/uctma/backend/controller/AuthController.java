package com.app.uctma.backend.controller;

import com.app.uctma.backend.dto.*;
import com.app.uctma.backend.model.ApiResponse;
import com.app.uctma.backend.service.AuthService;
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

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterReqDTO registerReqDTO) {
        ApiResponse<String> response = ApiResponse.success(authService.register(registerReqDTO), HttpStatus.CREATED);
        return ResponseEntity.status(response.code()).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResDTO>> login(@Valid @RequestBody LoginReqDTO loginReqDTO) {
        LoginResDTO loginResDTO = authService.login(loginReqDTO);
        return ResponseEntity.ok(ApiResponse.success(loginResDTO));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordReqDTO changePasswordReqDTO) {
        authService.changePassword(changePasswordReqDTO);
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
}
