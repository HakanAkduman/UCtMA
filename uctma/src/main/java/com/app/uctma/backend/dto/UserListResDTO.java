package com.app.uctma.backend.dto;

import com.app.uctma.backend.model.Role;

public record UserListResDTO(
        String email,
        Role role,
        boolean enabled,
        int failedLoginAttempts
) {}
