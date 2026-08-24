package com.javabank.util;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class generating thread-safe identifier strings for customers, accounts, and transactions.
 */
public final class IdGenerator {

    private IdGenerator() {
        throw new UnsupportedOperationException("Utility class should not be instantiated.");
    }

    public static String generateAccountNumber() {
        // Generates a 10-digit random number string
        long randomNum = ThreadLocalRandom.current().nextLong(1000000000L, 10000000000L);
        return String.valueOf(randomNum);
    }

    public static String generateTransactionId() {
        return "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static String generateCustomerId() {
        return "CUST-" + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
