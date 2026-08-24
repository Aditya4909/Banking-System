package com.javabank.transaction;

import com.javabank.exception.BankException;
import com.javabank.exception.InvalidTransferException;
import com.javabank.model.Account;

/**
 * Concrete transaction processor for transferring funds between accounts.
 */
public class TransferProcessor implements TransactionProcessor {
    private final Account sourceAccount;
    private final Account destinationAccount;
    private final double amount;

    public TransferProcessor(Account sourceAccount, Account destinationAccount, double amount) {
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
    }

    @Override
    public void execute() throws BankException {
        if (sourceAccount == null || destinationAccount == null) {
            throw new BankException("Source and destination accounts must not be null.");
        }
        if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) {
            throw new InvalidTransferException("Cannot transfer funds to the same account number: " + sourceAccount.getAccountNumber());
        }
        
        // Execute withdrawal from source
        sourceAccount.withdraw(amount);
        
        // Attempt deposit to destination
        try {
            destinationAccount.deposit(amount);
        } catch (Exception e) {
            // Rollback withdrawal if deposit fails (compensating transaction)
            try {
                sourceAccount.deposit(amount, "Rollback: Transfer destination failed");
            } catch (Exception rollbackEx) {
                throw new BankException("Critical transfer failure: Deposit aborted, rollback failed.", rollbackEx);
            }
            throw new InvalidTransferException("Transfer failed: Deposit to destination account aborted. Withdrawal rolled back. Error: " + e.getMessage());
        }
    }
}
