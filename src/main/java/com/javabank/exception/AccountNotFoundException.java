package com.javabank.exception;

/**
 * Thrown when an account lookup by ID or account number fails.
 */
public class AccountNotFoundException extends BankException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
