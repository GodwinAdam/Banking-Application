package com.bankingapp.exception;

import com.bankingapp.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Centralizes error handling for EVERY controller in the app. Instead of
 * each controller method wrapping its body in try/catch, we throw domain
 * exceptions from the service layer and let them bubble up here, where
 * Spring intercepts them based on their type and calls the matching
 * @ExceptionHandler method below.
 */
@RestControllerAdvice
// @RestControllerAdvice = @ControllerAdvice + @ResponseBody. It applies to
// every @RestController in the application context (global scope), and
// each handler method's return value is serialized straight to JSON, the
// same way a normal @RestController method's return value would be.
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    // Spring inspects the type of the exception that was thrown anywhere
    // inside a controller call and routes it to the handler whose
    // @ExceptionHandler value matches (or is a superclass of) that type.
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ApiError body = buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        ApiError body = buildError(HttpStatus.CONFLICT, ex.getMessage(), request, null);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiError> handleInsufficientBalance(InsufficientBalanceException ex,
                                                                HttpServletRequest request) {
        ApiError body = buildError(HttpStatus.CONFLICT, ex.getMessage(), request, null);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // Spring throws this automatically when a @Valid-annotated @RequestBody
    // fails one or more Bean Validation rules (@NotBlank, @Email, etc.)
    // BEFORE our controller method body even runs.
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                       HttpServletRequest request) {
        // ex.getBindingResult() holds one FieldError per failed constraint;
        // we flatten them into readable "field: message" strings.
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        ApiError body = buildError(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    // Thrown when the request body isn't valid JSON at all, or a field
    // can't be converted to the target type (e.g. accountType: "FOO" when
    // only SAVINGS/CURRENT exist).
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex,
                                                       HttpServletRequest request) {
        ApiError body = buildError(HttpStatus.BAD_REQUEST, "Malformed JSON request body", request, null);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    // The catch-all safety net: anything not handled above (a bug, a null
    // pointer, whatever) becomes a 500 instead of leaking a raw Java stack
    // trace to the client.
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        ApiError body = buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred", request, null);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ApiError buildError(HttpStatus status, String message, HttpServletRequest request,
                                 List<String> validationErrors) {
        return new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                validationErrors
        );
    }
}
