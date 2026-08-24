package com.javabank.exception;

/**
 * Thrown when transfer execution parameters violate security or logic constraints.
 * Implemented as a checked exception to roll back actions safely.
 */
public class InvalidTransferException extends BankException {
    public InvalidTransferException(String message) {
        super(message);
    }
}
