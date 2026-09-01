package com.javabank.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the owner of bank accounts.
 * Provides encapsulation, thread-safe collections addition, and cyclic-safe deep copying.
 */
public class User {
    private final String userId;
    private final String name;
    private final String email;
    @JsonIgnore
    private final List<Account> accounts;

    public User(String userId, String name, String email) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or empty.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.accounts = new ArrayList<>();
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @JsonIgnore
    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    /**
     * Thread-safe registration of accounts to the user profile cache.
     */
    public synchronized void addAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Cannot add a null account.");
        }
        if (!this.accounts.contains(account)) {
            this.accounts.add(account);
        }
    }

    /**
     * Shallow copy: duplicate the User and the accounts list container,
     * but point to the same Account references.
     */
    public synchronized User shallowCopy() {
        User copy = new User(this.userId, this.name, this.email);
        copy.accounts.addAll(this.accounts);
        return copy;
    }

    /**
     * Deep copy: duplicate the User and clone each individual Account,
     * re-binding the owner to the new user copy to avoid cyclic replication mismatches.
     */
    public synchronized User deepCopy() {
        User copy = new User(this.userId, this.name, this.email);
        for (Account account : this.accounts) {
            copy.addAccount(account.copy(copy));
        }
        return copy;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", accountsCount=" + accounts.size() +
                '}';
    }
}
