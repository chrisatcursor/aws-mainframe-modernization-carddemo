package com.carddemo.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation failed");
        ApiError payload = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", message);
        return ResponseEntity.badRequest().body(payload);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        ApiError payload = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getMessage());
        return ResponseEntity.badRequest().body(payload);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ValidationException ex) {
        ApiError payload = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", ex.getMessage());
        return ResponseEntity.badRequest().body(payload);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        ApiError payload = new ApiError(Instant.now(), HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(payload);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        ApiError payload = new ApiError(Instant.now(), HttpStatus.CONFLICT.value(), "CONFLICT", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(payload);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        ApiError payload = new ApiError(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(payload);
    }
}
