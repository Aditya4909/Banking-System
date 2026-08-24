package com.javabank.repository;

import com.javabank.model.Account;
import com.javabank.model.User;
import java.util.List;
import java.util.Map;

/**
 * Interface specific to Account storage actions.
 */
public interface AccountRepository extends Repository<Account, String> {
    List<Account> findByOwnerId(String ownerId) throws Exception;
    void initialize(Map<String, User> userCache) throws Exception;
}
