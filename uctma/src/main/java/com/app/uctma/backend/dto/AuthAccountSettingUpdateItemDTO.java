package com.app.uctma.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AuthAccountSettingUpdateItemDTO(
        @NotBlank String settingCode,
        boolean enabled,
        @Min(0) @Max(10080) int qty
) {
}
