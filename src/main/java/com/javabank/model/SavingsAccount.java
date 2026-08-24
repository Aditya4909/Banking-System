package com.javabank.model;

import com.javabank.exception.InsufficientBalanceException;
import com.javabank.exception.InvalidAmountException;

/**
 * Savings account with interest rate.
 * Prevents over-withdrawing beyond current balance.
 */
public class SavingsAccount extends Account {
    private final double interestRate;

    public SavingsAccount(String accountNumber, User owner, double balance, AccountStatus status, double interestRate) {
        super(accountNumber, owner, balance, status);
        if (interestRate < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative.");
        }
        this.interestRate = interestRate;
    }

    public SavingsAccount(SavingsAccount source) {
        super(source);
        this.interestRate = source.interestRate;
    }

    /**
     * Copy constructor with explicit owner assignment.
     */
    public SavingsAccount(SavingsAccount source, User newOwner) {
        super(source, newOwner);
        this.interestRate = source.interestRate;
    }

    public double getInterestRate() {
        return interestRate;
    }

    @Override
    public void withdraw(double amount, String description) throws InsufficientBalanceException, InvalidAmountException {
        if (getAccountStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAmountException("Cannot withdraw from a non-active account. Status: " + getAccountStatus());
        }
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be positive. Provided: " + amount);
        }
        if (this.balance < amount) {
            throw new InsufficientBalanceException("Insufficient balance. Savings accounts cannot have negative balance. Current: " + balance + ", Requested: " + amount);
        }
        this.balance -= amount;
        logTransaction(TransactionType.WITHDRAWAL, amount, description);
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }

    @Override
    public SavingsAccount copy() {
        User decoupledOwner = new User(this.getOwner().getUserId(), this.getOwner().getName(), this.getOwner().getEmail());
        return copy(decoupledOwner);
    }

    @Override
    public SavingsAccount copy(User newOwner) {
        return new SavingsAccount(this, newOwner);
    }

    @Override
    public SavingsAccount clone() {
        return (SavingsAccount) super.clone();
    }
}
