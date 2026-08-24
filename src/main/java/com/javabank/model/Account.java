package com.javabank.model;

import com.javabank.exception.InsufficientBalanceException;
import com.javabank.exception.InvalidAmountException;
import com.javabank.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract class representing a bank account.
 * Demonstrates Abstraction, Encapsulation, Copying/Cloning, and Method Overloading.
 */
public abstract class Account implements Copyable<Account>, Cloneable {
    private final String accountNumber;
    private final User owner;
    protected double balance;
    private final List<Transaction> transactions;
    private AccountStatus accountStatus;

    protected Account(String accountNumber, User owner, double balance, AccountStatus status) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be null or empty.");
        }
        if (owner == null) {
            throw new IllegalArgumentException("Account owner cannot be null.");
        }
        if (balance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.balance = balance;
        this.accountStatus = status != null ? status : AccountStatus.ACTIVE;
        this.transactions = new ArrayList<>();
    }

    /**
     * Copy Constructor performing a deep-copy of mutable members (User and Transaction List).
     */
    protected Account(Account source) {
        this.accountNumber = source.accountNumber;
        this.owner = new User(source.owner.getUserId(), source.owner.getName(), source.owner.getEmail());
        this.balance = source.balance;
        this.accountStatus = source.accountStatus;
        this.transactions = new ArrayList<>(source.transactions);
    }

    /**
     * Overloaded Copy Constructor that re-binds the owner reference to resolve cyclic deep copying.
     */
    protected Account(Account source, User newOwner) {
        this.accountNumber = source.accountNumber;
        this.owner = newOwner;
        this.balance = source.balance;
        this.accountStatus = source.accountStatus;
        this.transactions = new ArrayList<>(source.transactions);
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public User getOwner() {
        return owner;
    }

    public double getBalance() {
        return balance;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Account status cannot be null.");
        }
        this.accountStatus = status;
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    protected void logTransaction(TransactionType type, double amount, String description) {
        Transaction tx = new Transaction(
                IdGenerator.generateTransactionId(),
                type,
                amount,
                LocalDateTime.now(),
                this.accountNumber,
                null,
                description != null ? description : type.name()
        );
        this.transactions.add(tx);
    }

    public void deposit(double amount) throws InvalidAmountException {
        deposit(amount, "Deposit");
    }

    public void deposit(double amount, String description) throws InvalidAmountException {
        if (this.accountStatus != AccountStatus.ACTIVE) {
            throw new InvalidAmountException("Cannot deposit into a non-active account. Status: " + accountStatus);
        }
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be positive. Provided: " + amount);
        }
        this.balance += amount;
        logTransaction(TransactionType.DEPOSIT, amount, description);
    }

    public void withdraw(double amount) throws InsufficientBalanceException, InvalidAmountException {
        withdraw(amount, "Withdrawal");
    }

    public abstract void withdraw(double amount, String description) 
            throws InsufficientBalanceException, InvalidAmountException;

    public abstract String getAccountType();

    @Override
    public abstract Account copy();

    /**
     * Deep-clones the account using a specific designated User owner.
     */
    public abstract Account copy(User newOwner);

    @Override
    public Account clone() {
        try {
            Account cloned = (Account) super.clone();
            java.lang.reflect.Field field = Account.class.getDeclaredField("transactions");
            field.setAccessible(true);
            field.set(cloned, new ArrayList<>(this.transactions));
            return cloned;
        } catch (CloneNotSupportedException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Clone failed for account: " + accountNumber, e);
        }
    }
}
