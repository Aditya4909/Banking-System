package com.javabank.transaction;

import com.javabank.exception.BankException;
import com.javabank.model.Account;

/**
 * Concrete transaction processor for account deposits.
 */
public class DepositProcessor implements TransactionProcessor {
    private final Account account;
    private final double amount;

    public DepositProcessor(Account account, double amount) {
        this.account = account;
        this.amount = amount;
    }

    @Override
    public void execute() throws BankException {
        if (account == null) {
            throw new BankException("Account cannot be null.");
        }
        account.deposit(amount);
    }
}
