package com.javabank.service;

import com.javabank.model.Account;
import com.javabank.model.Transaction;
import com.javabank.model.TransactionType;
import com.javabank.model.User;
import com.javabank.transaction.TransactionAction;
import com.javabank.transaction.TransactionFilter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Unified service contract for all banking transactions, account maintenance, and user management.
 */
public interface BankService {
    
    void createUser(User user) throws Exception;

    User getUser(String userId) throws Exception;

    Account createAccount(String ownerId, String accountType, double initialBalance, double extraParam) throws Exception;

    Account createAccount(String ownerId, String accountType, double initialBalance) throws Exception;

    Account findAccount(String accountNumber) throws Exception;

    void deposit(String accountNumber, double amount, String description) throws Exception;

    void withdraw(String accountNumber, double amount, String description) throws Exception;

    void transfer(String sourceAccountNumber, String destinationAccountNumber, double amount, String description) throws Exception;

    List<Transaction> getTransactionHistory(String accountNumber) throws Exception;

    void closeAccount(String accountNumber) throws Exception;

    double getAccountBalance(String accountNumber) throws Exception;

    List<Account> getAllAccounts() throws Exception;

    List<Transaction> filterTransactions(String accountNumber, TransactionFilter filter) throws Exception;
    
    void executeAction(String accountNumber, TransactionAction action) throws Exception;

    Account createAccountSnapshot(String accountNumber) throws Exception;

    /**
     * Filters transactions using multiple optional parameters.
     */
    List<Transaction> getFilteredTransactions(String accountNumber, 
                                              TransactionType type, 
                                              Double minAmount, 
                                              Double maxAmount, 
                                              LocalDateTime start, 
                                              LocalDateTime end) throws Exception;
}
