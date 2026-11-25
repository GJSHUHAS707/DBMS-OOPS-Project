package com.cinephile.exception;

/**
 * Base user-defined checked exception for the Cinephile application.
 * Demonstrates CS23333 Unit III: Custom Exception Handling.
 */
public class CinephileException extends Exception {
    public CinephileException(String message) {
        super(message);
    }

    public CinephileException(String message, Throwable cause) {
        super(message, cause);
    }
}
