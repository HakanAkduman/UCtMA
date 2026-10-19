package com.app.uctma.backend.dto;

import jakarta.validation.constraints.NotNull;

public record UserEnabledUpdateReqDTO(@NotNull Boolean enabled) {
}
