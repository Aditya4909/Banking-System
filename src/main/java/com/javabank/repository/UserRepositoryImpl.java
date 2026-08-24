package com.javabank.repository;

import com.javabank.model.User;

/**
 * In-memory implementation of UserRepository.
 * Inherits generic CRUD storage behavior.
 */
public class UserRepositoryImpl extends InMemoryRepository<User, String> implements UserRepository {
    public UserRepositoryImpl() {
        super(User::getUserId); // Passes key extractor mapping to super constructor
    }
}
