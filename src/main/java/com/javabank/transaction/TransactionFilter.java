package com.javabank.transaction;

import com.javabank.model.Transaction;

/**
 * Domain-specific functional interface to filter transactions.
 * Enables the caller to pass predicate lambdas tailored to transaction data.
 */
@FunctionalInterface
public interface TransactionFilter {
    /**
     * Evaluates the transaction against custom filter criteria.
     *
     * @param transaction the transaction log entry.
     * @return true if matches, false otherwise.
     */
    boolean test(Transaction transaction);
}
