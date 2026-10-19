package com.app.uctma.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AuthAccountSettingsUpdateReqDTO(
        @NotEmpty List<@Valid AuthAccountSettingUpdateItemDTO> settings
) {
}
