package com.app.uctma.backend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int code,
        T data,
        ErrorResponse error
) {



    public static <T> ApiResponse<T> success(T data, HttpStatus status) {
        return new ApiResponse<>(true, status.value(), data, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, HttpStatus.OK);
    }

    public static <T> ApiResponse<T> success(HttpStatus status) {
        return new ApiResponse<>(true, status.value(), null, null);
    }



    public static <T> ApiResponse<T> error(String errorMessage, String path, HttpStatus status) {
        ErrorResponse err = ErrorResponse.of(errorMessage, path);
        return new ApiResponse<>(false, status.value(), null, err);
    }
}
