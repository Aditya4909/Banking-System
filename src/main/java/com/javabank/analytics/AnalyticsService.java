package com.javabank.analytics;

import com.javabank.model.Account;
import com.javabank.model.Transaction;
import com.javabank.model.TransactionType;
import com.javabank.model.User;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Unified Analytics engine interface utilizing Stream pipelines for statistics computation.
 * Extends ReportGenerator to support statement rendering.
 */
public interface AnalyticsService extends ReportGenerator {

    double calculateTotalDepositedAmount(List<Transaction> transactions);

    double calculateTotalWithdrawnAmount(List<Transaction> transactions);

    double calculateTotalTransferredAmount(List<Transaction> transactions);

    Optional<Transaction> findLargestTransaction(List<Transaction> transactions);

    Optional<Transaction> findSmallestTransaction(List<Transaction> transactions);

    double calculateAverageTransactionAmount(List<Transaction> transactions);

    long countTransactions(List<Transaction> transactions);

    List<Transaction> findTransactionsAbove(List<Transaction> transactions, double threshold);

    List<Transaction> findTransactionsWithinRange(List<Transaction> transactions, LocalDateTime start, LocalDateTime end);

    Map<TransactionType, List<Transaction>> groupTransactionsByType(List<Transaction> transactions);

    Map<TransactionType, Long> getTransactionCountsByType(List<Transaction> transactions);

    List<Transaction> getTop5LargestTransactions(List<Transaction> transactions);

    Map<Month, Double> calculateMonthlyTransactionSummary(List<Transaction> transactions);

    // Merged calculation methods
    double calculateTotalVaultBalance(List<Account> accounts);

    double calculateAverageBalance(List<Account> accounts);

    List<Account> findHighValueAccounts(List<Account> accounts, double threshold);

    List<Transaction> sortTransactionsChronologically(List<Transaction> transactions, boolean ascending);

    List<String> getUniqueUserEmails(List<User> users);
}
