package com.javabank.model;

import com.javabank.exception.InsufficientBalanceException;
import com.javabank.exception.InvalidAmountException;

/**
 * Current account with an overdraft limit.
 * Allows drawing down past zero balance, up to the overdraft limit.
 */
public class CurrentAccount extends Account {
    private final double overdraftLimit;

    public CurrentAccount(String accountNumber, User owner, double balance, AccountStatus status, double overdraftLimit) {
        super(accountNumber, owner, balance, status);
        if (overdraftLimit < 0) {
            throw new IllegalArgumentException("Overdraft limit cannot be negative.");
        }
        this.overdraftLimit = overdraftLimit;
    }

    public CurrentAccount(CurrentAccount source) {
        super(source);
        this.overdraftLimit = source.overdraftLimit;
    }

    /**
     * Copy constructor with explicit owner assignment.
     */
    public CurrentAccount(CurrentAccount source, User newOwner) {
        super(source, newOwner);
        this.overdraftLimit = source.overdraftLimit;
    }

    public double getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    public void withdraw(double amount, String description) throws InsufficientBalanceException, InvalidAmountException {
        if (getAccountStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAmountException("Cannot withdraw from a non-active account. Status: " + getAccountStatus());
        }
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be positive. Provided: " + amount);
        }
        if (this.balance + this.overdraftLimit < amount) {
            throw new InsufficientBalanceException("Insufficient balance. Withdrawal would exceed overdraft limit of " 
                    + overdraftLimit + ". Current balance: " + balance + ", Requested: " + amount);
        }
        this.balance -= amount;
        logTransaction(TransactionType.WITHDRAWAL, amount, description);
    }

    @Override
    public String getAccountType() {
        return "Current";
    }

    @Override
    public CurrentAccount copy() {
        User decoupledOwner = new User(this.getOwner().getUserId(), this.getOwner().getName(), this.getOwner().getEmail());
        return copy(decoupledOwner);
    }

    @Override
    public CurrentAccount copy(User newOwner) {
        return new CurrentAccount(this, newOwner);
    }

    @Override
    public CurrentAccount clone() {
        return (CurrentAccount) super.clone();
    }
}
