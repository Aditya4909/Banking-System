package com.javabank.web.dto;

import com.javabank.model.Transaction;

/**
 * Clean data transfer record for transaction logs.
 */
public record TransactionDTO(
        String transactionId,
        String type,
        double amount,
        String timestamp,
        String sourceAccountNumber,
        String destinationAccountNumber,
        String status,
        String description
) {
    public static TransactionDTO from(Transaction t) {
        if (t == null) return null;
        return new TransactionDTO(
                t.getTransactionId(),
                t.getType() != null ? t.getType().name() : "UNKNOWN",
                t.getAmount(),
                t.getTimestamp() != null ? t.getTimestamp().toString() : "",
                t.getSourceAccountNumber(),
                t.getDestinationAccountNumber(),
                t.getStatus() != null ? t.getStatus().name() : "SUCCESS",
                t.getDescription()
        );
    }
}
