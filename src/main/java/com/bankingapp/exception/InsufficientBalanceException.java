package com.bankingapp.exception;

/**
 * Thrown when a withdrawal or transfer would take an account's balance
 * below zero. Kept as its own type (rather than reusing a generic
 * exception) so GlobalExceptionHandler can map it to its own distinct HTTP
 * status (409 Conflict - the request is well-formed but conflicts with the
 * account's current state).
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
