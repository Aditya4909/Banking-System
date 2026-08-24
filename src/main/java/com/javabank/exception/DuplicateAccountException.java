package com.javabank.exception;

/**
 * Thrown when trying to create or save an account with a number that already exists.
 * Implemented as a checked exception to prompt for correction or unique account numbers.
 */
public class DuplicateAccountException extends BankException {
    public DuplicateAccountException(String message) {
        super(message);
    }
}
