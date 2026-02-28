package com.expensesplitter.exception;

/**
 * Exception thrown when an invalid operation is attempted.
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
