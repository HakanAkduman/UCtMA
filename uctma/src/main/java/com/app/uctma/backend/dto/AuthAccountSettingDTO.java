package com.app.uctma.backend.dto;

public record AuthAccountSettingDTO(
        Long id,
        String settingCode,
        String description,
        boolean enabled,
        int qty
) {
}
