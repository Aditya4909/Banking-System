package com.javabank.transaction;

import com.javabank.model.Account;

/**
 * Domain-specific functional interface to apply custom operations on bank accounts.
 * Enables passing executable account logic as parameters.
 */
@FunctionalInterface
public interface TransactionAction {
    /**
     * Executes custom mutations or validations directly on the target account.
     *
     * @param account the account to be operated on.
     * @throws Exception if operational validation fails.
     */
    void perform(Account account) throws Exception;
}
