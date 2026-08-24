package com.javabank.service;

import com.javabank.exception.*;
import com.javabank.model.*;
import com.javabank.repository.*;
import com.javabank.transaction.*;
import com.javabank.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Unified implementation of BankService.
 * Orchestrates domain repositories, transaction processors, and file-based persistence.
 */
public class BankServiceImpl implements BankService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PersistenceService persistenceService;

    public BankServiceImpl(UserRepository userRepository, 
                           AccountRepository accountRepository, 
                           TransactionRepository transactionRepository,
                           PersistenceService persistenceService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.persistenceService = persistenceService;

        if (persistenceService != null) {
            try {
                loadAllData();
                registerShutdownHook();
            } catch (Exception e) {
                System.err.println("Startup Error: Failed to restore session from file database.");
                e.printStackTrace();
            }
        }
    }

    private void loadAllData() throws Exception {
        Map<String, User> userCache = new HashMap<>();
        Map<String, Account> accountCache = new HashMap<>();
        List<Transaction> transactionCache = new ArrayList<>();

        persistenceService.loadAll(userCache, accountCache, transactionCache);

        for (User user : userCache.values()) {
            userRepository.save(user);
        }
        for (Account account : accountCache.values()) {
            accountRepository.save(account);
        }
        for (Transaction tx : transactionCache) {
            transactionRepository.save(tx);
        }
    }

    private void triggerSave() {
        if (persistenceService != null) {
            try {
                Map<String, User> usersMap = new HashMap<>();
                for (User u : userRepository.findAll()) {
                    usersMap.put(u.getUserId(), u);
                }
                List<Account> accounts = accountRepository.findAll();
                List<Transaction> transactions = transactionRepository.findAll();

                persistenceService.saveAll(usersMap, accounts, transactions);
            } catch (Exception e) {
                System.err.println("Persistence Warning: Real-time serialization to disk failed.");
                e.printStackTrace();
            }
        }
    }

    private void registerShutdownHook() {
        if (persistenceService instanceof FilePersistenceService) {
            ((FilePersistenceService) persistenceService).registerShutdownHook(
                    () -> {
                        Map<String, User> usersMap = new HashMap<>();
                        try {
                            for (User u : userRepository.findAll()) {
                                usersMap.put(u.getUserId(), u);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return usersMap;
                    },
                    () -> {
                        try {
                            return accountRepository.findAll();
                        } catch (Exception e) {
                            return Collections.emptyList();
                        }
                    },
                    () -> {
                        try {
                            return transactionRepository.findAll();
                        } catch (Exception e) {
                            return Collections.emptyList();
                        }
                    }
            );
        }
    }

    @Override
    public synchronized void createUser(User user) throws Exception {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        if (userRepository.findById(user.getUserId()).isPresent()) {
            throw new BankException("User with ID " + user.getUserId() + " already exists.");
        }
        userRepository.save(user);
        triggerSave();
    }

    @Override
    public User getUser(String userId) throws Exception {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BankException("User not found: " + userId));
    }

    @Override
    public synchronized Account createAccount(String ownerId, String accountType, double initialBalance, double extraParam) throws Exception {
        User owner = getUser(ownerId);
        String accountNo = IdGenerator.generateAccountNumber();

        if (accountRepository.findById(accountNo).isPresent()) {
            throw new DuplicateAccountException("Generated account number " + accountNo + " already exists. Please retry.");
        }

        Account account;
        if ("Savings".equalsIgnoreCase(accountType)) {
            account = new SavingsAccount(accountNo, owner, initialBalance, AccountStatus.ACTIVE, extraParam);
        } else if ("Current".equalsIgnoreCase(accountType)) {
            account = new CurrentAccount(accountNo, owner, initialBalance, AccountStatus.ACTIVE, extraParam);
        } else {
            throw new BankException("Invalid account type: " + accountType);
        }

        accountRepository.save(account);
        owner.addAccount(account);
        triggerSave();
        return account;
    }

    @Override
    public Account createAccount(String ownerId, String accountType, double initialBalance) throws Exception {
        double defaultExtra = "Savings".equalsIgnoreCase(accountType) ? 0.015 : 500.0;
        return createAccount(ownerId, accountType, initialBalance, defaultExtra);
    }

    @Override
    public Account findAccount(String accountNumber) throws Exception {
        return accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account number " + accountNumber + " not found."));
    }

    @Override
    public synchronized void deposit(String accountNumber, double amount, String description) throws Exception {
        Account account = findAccount(accountNumber);
        
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be positive. Provided: " + amount);
        }

        TransactionProcessor depositProcessor = new DepositProcessor(account, amount);
        depositProcessor.execute();
        
        accountRepository.save(account);

        Transaction tx = new Transaction(
                IdGenerator.generateTransactionId(),
                TransactionType.DEPOSIT,
                amount,
                LocalDateTime.now(),
                accountNumber,
                null,
                description != null ? description : "Deposit of " + amount
        );
        transactionRepository.save(tx);
        triggerSave();
    }

    @Override
    public synchronized void withdraw(String accountNumber, double amount, String description) throws Exception {
        Account account = findAccount(accountNumber);

        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be positive. Provided: " + amount);
        }

        TransactionProcessor withdrawProcessor = new WithdrawalProcessor(account, amount);
        withdrawProcessor.execute();

        accountRepository.save(account);

        Transaction tx = new Transaction(
                IdGenerator.generateTransactionId(),
                TransactionType.WITHDRAWAL,
                amount,
                LocalDateTime.now(),
                accountNumber,
                null,
                description != null ? description : "Withdrawal of " + amount
        );
        transactionRepository.save(tx);
        triggerSave();
    }

    @Override
    public synchronized void transfer(String sourceAccountNumber, String destinationAccountNumber, 
                                      double amount, String description) throws Exception {
        Account src = findAccount(sourceAccountNumber);
        Account dest = findAccount(destinationAccountNumber);

        if (amount <= 0) {
            throw new InvalidAmountException("Transfer amount must be positive. Provided: " + amount);
        }
        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            throw new InvalidTransferException("Cannot transfer to the same account: " + sourceAccountNumber);
        }

        TransactionProcessor transferProcessor = new TransferProcessor(src, dest, amount);
        transferProcessor.execute();

        accountRepository.save(src);
        accountRepository.save(dest);

        Transaction tx = new Transaction(
                IdGenerator.generateTransactionId(),
                TransactionType.TRANSFER,
                amount,
                LocalDateTime.now(),
                sourceAccountNumber,
                destinationAccountNumber,
                description != null ? description : "Transfer to " + destinationAccountNumber
        );
        transactionRepository.save(tx);
        triggerSave();
    }

    @Override
    public List<Transaction> getTransactionHistory(String accountNumber) throws Exception {
        findAccount(accountNumber);
        return transactionRepository.findByAccountNumber(accountNumber).stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void closeAccount(String accountNumber) throws Exception {
        Account account = findAccount(accountNumber);
        if (account.getBalance() != 0) {
            throw new BankException("Cannot close account " + accountNumber + " with non-zero balance: " + account.getBalance());
        }
        account.setAccountStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
        triggerSave();
    }

    @Override
    public double getAccountBalance(String accountNumber) throws Exception {
        Account account = findAccount(accountNumber);
        return account.getBalance();
    }

    @Override
    public List<Account> getAllAccounts() throws Exception {
        return accountRepository.findAll();
    }

    @Override
    public List<Transaction> filterTransactions(String accountNumber, TransactionFilter filter) throws Exception {
        List<Transaction> history = getTransactionHistory(accountNumber);
        return history.stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void executeAction(String accountNumber, TransactionAction action) throws Exception {
        Account account = findAccount(accountNumber);
        action.perform(account);
        accountRepository.save(account);
        triggerSave();
    }

    @Override
    public Account createAccountSnapshot(String accountNumber) throws Exception {
        Account original = findAccount(accountNumber);
        return original.copy();
    }

    @Override
    public List<Transaction> getFilteredTransactions(String accountNumber, 
                                                     TransactionType type, 
                                                     Double minAmount, 
                                                     Double maxAmount, 
                                                     LocalDateTime start, 
                                                     LocalDateTime end) throws Exception {
        List<Transaction> history = getTransactionHistory(accountNumber);
        return history.stream()
                .filter(tx -> type == null || tx.getType() == type)
                .filter(tx -> minAmount == null || tx.getAmount() >= minAmount)
                .filter(tx -> maxAmount == null || tx.getAmount() <= maxAmount)
                .filter(tx -> start == null || !tx.getTimestamp().isBefore(start))
                .filter(tx -> end == null || !tx.getTimestamp().isAfter(end))
                .collect(Collectors.toList());
    }
}
