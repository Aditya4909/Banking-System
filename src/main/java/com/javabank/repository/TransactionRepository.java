package com.javabank.repository;

import com.javabank.model.Transaction;
import java.util.List;

/**
 * Interface specific to Transaction logging and retrieval.
 */
public interface TransactionRepository extends Repository<Transaction, String> {
    List<Transaction> findByAccountNumber(String accountNumber) throws Exception;
}
