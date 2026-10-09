package com.app.uctma.backend.model;

import java.time.LocalDateTime;

public record ErrorResponse(
        String errorMessage,
        String path,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String errorMessage, String path) {
        return new ErrorResponse(errorMessage, path, LocalDateTime.now());
    }
}