package com.javabank.transaction;

import com.javabank.exception.BankException;

/**
 * Functional interface for executing banking transactions.
 * Highlights custom functional interface requirements.
 */
@FunctionalInterface
public interface TransactionProcessor {
    void execute() throws BankException;
}
