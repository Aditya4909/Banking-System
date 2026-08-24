package com.javabank.exception;

/**
 * Thrown when a deposit or withdrawal amount is invalid (e.g. negative or zero value).
 * Implemented as a checked exception to capture invalid user input.
 */
public class InvalidAmountException extends BankException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
