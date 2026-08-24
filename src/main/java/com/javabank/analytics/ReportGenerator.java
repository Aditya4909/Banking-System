package com.javabank.analytics;

import com.javabank.model.Account;
import java.util.List;

/**
 * Interface contract defining formatting rules for financial reports and account statements.
 */
public interface ReportGenerator {
    /**
     * Generates a detailed statement for a single bank account.
     */
    String generateAccountStatement(Account account);

    /**
     * Generates a high-level vault asset summary for a list of accounts.
     */
    String generateVaultSummary(List<Account> accounts);
}
