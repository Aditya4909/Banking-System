package com.javabank.exception;

/**
 * Thrown when a withdrawal or transfer amount exceeds the account's available balance.
 * Implemented as a checked exception since this is a recoverable business error.
 */
public class InsufficientBalanceException extends BankException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
