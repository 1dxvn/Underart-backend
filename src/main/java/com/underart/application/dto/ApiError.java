package com.underart.application.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(
        int status,
        String error,
        String message,
        String path,
        String requestId,
        Instant timestamp,
        List<FieldError> fieldErrors) {

    public ApiError {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static ApiError of(int status, String error, String message, String path, String requestId) {
        return new ApiError(status, error, message, path, requestId, Instant.now(), List.of());
    }

    public record FieldError(String field, String message) {
    }
}
