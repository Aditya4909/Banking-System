package com.javabank.repository;

import com.javabank.model.Transaction;
import java.util.List;

/**
 * In-memory implementation of TransactionRepository.
 * Inherits generic Map storage and provides account transaction filtering.
 */
public class InMemoryTransactionRepository extends InMemoryRepository<Transaction, String> implements TransactionRepository {
    
    public InMemoryTransactionRepository() {
        super(Transaction::getTransactionId);
    }

    @Override
    public List<Transaction> findByAccountNumber(String accountNumber) {
        return find(tx -> tx.getSourceAccountNumber().equals(accountNumber) ||
                (tx.getDestinationAccountNumber() != null && tx.getDestinationAccountNumber().equals(accountNumber)));
    }
}
