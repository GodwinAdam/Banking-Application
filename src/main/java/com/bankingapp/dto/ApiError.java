package com.bankingapp.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A single, predictable JSON shape for every error this API ever returns,
 * so client code only needs to write one error-parsing branch instead of
 * guessing the shape per-endpoint.
 */
public class ApiError {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<String> validationErrors; // null unless it was a validation (400) failure

    public ApiError() {
    }

    public ApiError(LocalDateTime timestamp, int status, String error, String message,
                     String path, List<String> validationErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.validationErrors = validationErrors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
