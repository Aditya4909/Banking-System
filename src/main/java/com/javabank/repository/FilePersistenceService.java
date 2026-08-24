package com.javabank.repository;

import com.javabank.model.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

/**
 * File-based CSV implementation of PersistenceService.
 * Handles missing files, data corruption recovery, and registers a JVM shutdown hook using Suppliers.
 */
public class FilePersistenceService implements PersistenceService {
    private final Path usersPath;
    private final Path accountsPath;
    private final Path transactionsPath;

    public FilePersistenceService(String dataDirName) throws IOException {
        Path dataDir = Paths.get(dataDirName);
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }
        this.usersPath = dataDir.resolve("users.csv");
        this.accountsPath = dataDir.resolve("accounts.csv");
        this.transactionsPath = dataDir.resolve("transactions.csv");

        ensureFileExists(usersPath);
        ensureFileExists(accountsPath);
        ensureFileExists(transactionsPath);
    }

    private void ensureFileExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
    }

    @Override
    public synchronized void saveAll(Map<String, User> users, List<Account> accounts, List<Transaction> transactions) throws IOException {
        // Save Users
        try (BufferedWriter writer = Files.newBufferedWriter(usersPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (User user : users.values()) {
                writer.write(String.join(",", user.getUserId(), escapeCsv(user.getName()), escapeCsv(user.getEmail())));
                writer.newLine();
            }
        }

        // Save Accounts
        try (BufferedWriter writer = Files.newBufferedWriter(accountsPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Account account : accounts) {
                double extra = 0;
                if (account instanceof SavingsAccount) {
                    extra = ((SavingsAccount) account).getInterestRate();
                } else if (account instanceof CurrentAccount) {
                    extra = ((CurrentAccount) account).getOverdraftLimit();
                }
                writer.write(String.join(",",
                        account.getAccountNumber(),
                        account.getOwner().getUserId(),
                        String.valueOf(account.getBalance()),
                        account.getAccountStatus().name(),
                        account.getAccountType(),
                        String.valueOf(extra)
                ));
                writer.newLine();
            }
        }

        // Save Transactions
        try (BufferedWriter writer = Files.newBufferedWriter(transactionsPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Transaction tx : transactions) {
                writer.write(String.join(",",
                        tx.getTransactionId(),
                        tx.getType().name(),
                        String.valueOf(tx.getAmount()),
                        tx.getTimestamp().toString(),
                        tx.getSourceAccountNumber(),
                        tx.getDestinationAccountNumber() == null ? "null" : tx.getDestinationAccountNumber(),
                        escapeCsv(tx.getDescription()),
                        tx.getStatus().name()
                ));
                writer.newLine();
            }
        }
    }

    @Override
    public synchronized void loadAll(Map<String, User> userCache, Map<String, Account> accountCache, List<Transaction> transactionCache) throws IOException {
        userCache.clear();
        accountCache.clear();
        transactionCache.clear();

        // 1. Load Users
        try (BufferedReader reader = Files.newBufferedReader(usersPath)) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;
                try {
                    String[] parts = line.split(",");
                    if (parts.length < 3) {
                        throw new IllegalArgumentException("Insufficient columns.");
                    }
                    User user = new User(parts[0], unescapeCsv(parts[1]), unescapeCsv(parts[2]));
                    userCache.put(user.getUserId(), user);
                } catch (Exception e) {
                    System.err.println("File Corruption Warning: Skipped invalid line " + lineNum + " in users.csv. Error: " + e.getMessage());
                }
            }
        }

        // 2. Load Accounts (depends on loaded Users)
        try (BufferedReader reader = Files.newBufferedReader(accountsPath)) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;
                try {
                    String[] parts = line.split(",");
                    if (parts.length < 6) {
                        throw new IllegalArgumentException("Insufficient columns.");
                    }
                    String accountNumber = parts[0];
                    String ownerId = parts[1];
                    double balance = Double.parseDouble(parts[2]);
                    AccountStatus status = AccountStatus.valueOf(parts[3]);
                    String type = parts[4];
                    double extra = Double.parseDouble(parts[5]);

                    User owner = userCache.get(ownerId);
                    if (owner == null) {
                        throw new IllegalArgumentException("Owner ID " + ownerId + " not found in user database.");
                    }

                    Account account;
                    if ("Savings".equalsIgnoreCase(type)) {
                        account = new SavingsAccount(accountNumber, owner, balance, status, extra);
                    } else if ("Current".equalsIgnoreCase(type)) {
                        account = new CurrentAccount(accountNumber, owner, balance, status, extra);
                    } else {
                        throw new IllegalArgumentException("Unknown account type: " + type);
                    }

                    accountCache.put(account.getAccountNumber(), account);
                    owner.addAccount(account);
                } catch (Exception e) {
                    System.err.println("File Corruption Warning: Skipped invalid line " + lineNum + " in accounts.csv. Error: " + e.getMessage());
                }
            }
        }

        // 3. Load Transactions
        try (BufferedReader reader = Files.newBufferedReader(transactionsPath)) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;
                try {
                    String[] parts = line.split(",", -1);
                    if (parts.length < 7) {
                        throw new IllegalArgumentException("Insufficient columns.");
                    }
                    String transactionId = parts[0];
                    TransactionType type = TransactionType.valueOf(parts[1]);
                    double amount = Double.parseDouble(parts[2]);
                    LocalDateTime timestamp = LocalDateTime.parse(parts[3]);
                    String sourceAcc = parts[4];
                    String destAcc = parts[5].equals("null") ? null : parts[5];
                    String desc = unescapeCsv(parts[6]);
                    
                    TransactionStatus status = TransactionStatus.SUCCESS;
                    if (parts.length >= 8) {
                        status = TransactionStatus.valueOf(parts[7]);
                    }

                    Transaction tx = new Transaction(transactionId, type, amount, timestamp, sourceAcc, destAcc, desc, status);
                    transactionCache.add(tx);
                    
                    // Associate loaded transactions to in-memory accounts list if they exist
                    Account src = accountCache.get(sourceAcc);
                    if (src != null) {
                        java.lang.reflect.Field field = Account.class.getDeclaredField("transactions");
                        field.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        List<Transaction> txList = (List<Transaction>) field.get(src);
                        txList.add(tx);
                    }
                    if (destAcc != null) {
                        Account dst = accountCache.get(destAcc);
                        if (dst != null) {
                            java.lang.reflect.Field field = Account.class.getDeclaredField("transactions");
                            field.setAccessible(true);
                            @SuppressWarnings("unchecked")
                            List<Transaction> txList = (List<Transaction>) field.get(dst);
                            txList.add(tx);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("File Corruption Warning: Skipped invalid line " + lineNum + " in transactions.csv. Error: " + e.getMessage());
                }
            }
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace(",", "\\,");
    }

    private String unescapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\\,", ",");
    }

    /**
     * Registers a JVM shutdown hook using Suppliers to atomically serialize the latest runtime states.
     */
    public void registerShutdownHook(Supplier<Map<String, User>> usersSupplier,
                                     Supplier<List<Account>> accountsSupplier,
                                     Supplier<List<Transaction>> transactionsSupplier) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Shutdown Hook: Auto-saving JavaBank session data...");
                saveAll(usersSupplier.get(), accountsSupplier.get(), transactionsSupplier.get());
                System.out.println("Shutdown Hook: Session backup completed successfully.");
            } catch (Exception e) {
                System.err.println("Shutdown Hook Error: Failed to execute automatic session commit.");
                e.printStackTrace();
            }
        }));
    }
}
