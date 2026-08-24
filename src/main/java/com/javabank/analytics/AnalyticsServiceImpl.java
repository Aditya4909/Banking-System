package com.javabank.analytics;

import com.javabank.model.Account;
import com.javabank.model.Transaction;
import com.javabank.model.TransactionType;
import com.javabank.model.User;
import com.javabank.util.CurrencyFormatter;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Concrete implementation of AnalyticsService using Java Stream API pipelines.
 * Enforces optimized primitive mapping streams to eliminate autoboxing overhead.
 */
public class AnalyticsServiceImpl implements AnalyticsService {

    @Override
    public double calculateTotalDepositedAmount(List<Transaction> transactions) {
        // Optimized: mapToDouble avoids heap boxing Double objects
        return transactions.stream()
                .filter(tx -> tx.getType() == TransactionType.DEPOSIT)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    @Override
    public double calculateTotalWithdrawnAmount(List<Transaction> transactions) {
        return transactions.stream()
                .filter(tx -> tx.getType() == TransactionType.WITHDRAWAL)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    @Override
    public double calculateTotalTransferredAmount(List<Transaction> transactions) {
        return transactions.stream()
                .filter(tx -> tx.getType() == TransactionType.TRANSFER)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    @Override
    public Optional<Transaction> findLargestTransaction(List<Transaction> transactions) {
        return transactions.stream()
                .max(Comparator.comparingDouble(Transaction::getAmount));
    }

    @Override
    public Optional<Transaction> findSmallestTransaction(List<Transaction> transactions) {
        return transactions.stream()
                .min(Comparator.comparingDouble(Transaction::getAmount));
    }

    @Override
    public double calculateAverageTransactionAmount(List<Transaction> transactions) {
        return transactions.stream()
                .mapToDouble(Transaction::getAmount)
                .average()
                .orElse(0.0);
    }

    @Override
    public long countTransactions(List<Transaction> transactions) {
        return transactions.stream()
                .count();
    }

    @Override
    public List<Transaction> findTransactionsAbove(List<Transaction> transactions, double threshold) {
        return transactions.stream()
                .filter(tx -> tx.getAmount() > threshold)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findTransactionsWithinRange(List<Transaction> transactions, LocalDateTime start, LocalDateTime end) {
        return transactions.stream()
                .filter(tx -> !tx.getTimestamp().isBefore(start) && !tx.getTimestamp().isAfter(end))
                .collect(Collectors.toList());
    }

    @Override
    public Map<TransactionType, List<Transaction>> groupTransactionsByType(List<Transaction> transactions) {
        return transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getType));
    }

    @Override
    public Map<TransactionType, Long> getTransactionCountsByType(List<Transaction> transactions) {
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getType,
                        Collectors.counting()
                ));
    }

    @Override
    public List<Transaction> getTop5LargestTransactions(List<Transaction> transactions) {
        return transactions.stream()
                .sorted(Comparator.comparingDouble(Transaction::getAmount).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Month, Double> calculateMonthlyTransactionSummary(List<Transaction> transactions) {
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        tx -> tx.getTimestamp().getMonth(),
                        Collectors.summingDouble(Transaction::getAmount)
                ));
    }

    // Merged implementations from FinancialAnalytics
    @Override
    public double calculateTotalVaultBalance(List<Account> accounts) {
        return accounts.stream()
                .mapToDouble(Account::getBalance)
                .sum();
    }

    @Override
    public double calculateAverageBalance(List<Account> accounts) {
        return accounts.stream()
                .mapToDouble(Account::getBalance)
                .average()
                .orElse(0.0);
    }

    @Override
    public List<Account> findHighValueAccounts(List<Account> accounts, double threshold) {
        return accounts.stream()
                .filter(acc -> acc.getBalance() >= threshold)
                .sorted(Comparator.comparingDouble(Account::getBalance).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> sortTransactionsChronologically(List<Transaction> transactions, boolean ascending) {
        Comparator<Transaction> comparator = Comparator.comparing(Transaction::getTimestamp);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return transactions.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getUniqueUserEmails(List<User> users) {
        return users.stream()
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public String generateAccountStatement(Account account) {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================\n");
        sb.append("         ACCOUNT STATEMENT          \n");
        sb.append("====================================\n");
        sb.append("Account Number: ").append(account.getAccountNumber()).append("\n");
        sb.append("Owner:          ").append(account.getOwner().getName()).append("\n");
        sb.append("Status:         ").append(account.getAccountStatus()).append("\n");
        sb.append("Type:           ").append(account.getAccountType()).append("\n");
        sb.append("Current Balance:").append(CurrencyFormatter.formatUSD(account.getBalance())).append("\n");
        sb.append("------------------------------------\n");
        sb.append("Transaction History:\n");
        if (account.getTransactions().isEmpty()) {
            sb.append("No transactions recorded.\n");
        } else {
            for (Transaction tx : account.getTransactions()) {
                sb.append(String.format("%s | %-10s | %10s | %s\n",
                        tx.getTimestamp().toLocalDate(),
                        tx.getType(),
                        CurrencyFormatter.formatUSD(tx.getAmount()),
                        tx.getDescription()
                ));
            }
        }
        sb.append("====================================\n");
        return sb.toString();
    }

    @Override
    public String generateVaultSummary(List<Account> accounts) {
        double totalBalance = calculateTotalVaultBalance(accounts);
        double avgBalance = calculateAverageBalance(accounts);
        StringBuilder sb = new StringBuilder();
        sb.append("====================================\n");
        sb.append("        SYSTEM VAULT SUMMARY        \n");
        sb.append("====================================\n");
        sb.append("Total Assets:    ").append(CurrencyFormatter.formatUSD(totalBalance)).append("\n");
        sb.append("Average Balance: ").append(CurrencyFormatter.formatUSD(avgBalance)).append("\n");
        sb.append("Total Accounts:  ").append(accounts.size()).append("\n");
        sb.append("====================================\n");
        return sb.toString();
    }
}
