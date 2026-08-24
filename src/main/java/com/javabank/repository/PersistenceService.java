package com.javabank.repository;

import com.javabank.model.Account;
import com.javabank.model.Transaction;
import com.javabank.model.User;

import java.util.List;
import java.util.Map;

/**
 * Service contract responsible for centralized file storage and data lifecycle operations.
 * Decouples file writing/reading from repositories and services.
 */
public interface PersistenceService {

    /**
     * Serializes all cached database states to local disk.
     */
    void saveAll(Map<String, User> users, List<Account> accounts, List<Transaction> transactions) throws Exception;

    /**
     * De-serializes file records and populates in-memory database caches on startup.
     */
    void loadAll(Map<String, User> userCache, Map<String, Account> accountCache, List<Transaction> transactionCache) throws Exception;
}
