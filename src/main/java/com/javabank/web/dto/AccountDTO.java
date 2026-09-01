package com.javabank.web.dto;

import com.javabank.model.Account;
import com.javabank.model.CurrentAccount;
import com.javabank.model.SavingsAccount;

/**
 * Clean data transfer record for Bank Accounts.
 */
public record AccountDTO(
        String accountNumber,
        String accountType,
        double balance,
        String accountStatus,
        Double interestRate,
        Double overdraftLimit
) {
    public static AccountDTO from(Account a) {
        if (a == null) return null;
        Double interest = (a instanceof SavingsAccount sa) ? sa.getInterestRate() : null;
        Double overdraft = (a instanceof CurrentAccount ca) ? ca.getOverdraftLimit() : null;
        return new AccountDTO(
                a.getAccountNumber(),
                a.getAccountType(),
                a.getBalance(),
                a.getAccountStatus() != null ? a.getAccountStatus().name() : "ACTIVE",
                interest,
                overdraft
        );
    }
}
