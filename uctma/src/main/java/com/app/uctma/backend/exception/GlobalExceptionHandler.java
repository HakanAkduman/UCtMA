package com.app.uctma.backend.exception;


import com.app.uctma.backend.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.error(
                ex.getMessage(),
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationError(
            MethodArgumentNotValidException err,
            HttpServletRequest request) {

        var fieldErrors = err.getBindingResult().getFieldErrors();

        String errorMessage = "Invalid request parameters.";

        if (!fieldErrors.isEmpty()) {
            errorMessage = fieldErrors.stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        }

        ApiResponse<Void> response = ApiResponse.error(
                errorMessage,
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }





}
