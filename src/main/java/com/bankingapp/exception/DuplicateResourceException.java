package com.bankingapp.exception;

/**
 * Thrown when trying to create something that must be unique (e.g. a
 * customer email that already exists) and it already does.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
