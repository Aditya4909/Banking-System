package com.javabank.exception;

/**
 * Thrown when credentials validation, PIN validation, or role authentication checks fail.
 * Implemented as a checked exception to block unauthorized actions.
 */
public class AuthenticationException extends BankException {
    public AuthenticationException(String message) {
        super(message);
    }
}
