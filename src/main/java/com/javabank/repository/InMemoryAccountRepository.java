package com.javabank.repository;

import com.javabank.model.Account;
import com.javabank.model.User;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of AccountRepository.
 * Inherits generic Map storage and adds Owner queries.
 */
public class InMemoryAccountRepository extends InMemoryRepository<Account, String> implements AccountRepository {
    
    public InMemoryAccountRepository() {
        super(Account::getAccountNumber);
    }

    @Override
    public List<Account> findByOwnerId(String ownerId) {
        return find(acc -> acc.getOwner().getUserId().equals(ownerId));
    }

    @Override
    public void initialize(Map<String, User> userCache) {
        // No-op for purely in-memory repository
    }
}
