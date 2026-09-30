package com.bankingapp.exception;

/**
 * Thrown whenever a lookup (by id, email, or account number) finds nothing.
 * Extends RuntimeException (an "unchecked" exception) rather than Exception,
 * so service methods that throw it don't force every caller up the stack to
 * add "throws ResourceNotFoundException" or wrap every call in try/catch -
 * it propagates automatically until GlobalExceptionHandler catches it.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
