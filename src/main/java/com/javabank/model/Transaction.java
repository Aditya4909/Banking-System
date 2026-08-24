package com.javabank.model;

import java.time.LocalDateTime;

/**
 * Immutable model class representing a transaction log entry.
 */
public final class Transaction {
    private final String transactionId;
    private final TransactionType type;
    private final double amount;
    private final LocalDateTime timestamp;
    private final String sourceAccountNumber;
    private final String destinationAccountNumber; // Nullable if not a transfer
    private final String description;
    private final TransactionStatus status;

    public Transaction(String transactionId, TransactionType type, double amount, 
                       LocalDateTime timestamp, String sourceAccountNumber, 
                       String destinationAccountNumber, String description, TransactionStatus status) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.description = description;
        this.status = status != null ? status : TransactionStatus.SUCCESS;
    }

    public Transaction(String transactionId, TransactionType type, double amount, 
                       LocalDateTime timestamp, String sourceAccountNumber, 
                       String destinationAccountNumber, String description) {
        this(transactionId, type, amount, timestamp, sourceAccountNumber, destinationAccountNumber, description, TransactionStatus.SUCCESS);
    }

    public String getTransactionId() {
        return transactionId;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public String getDescription() {
        return description;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + transactionId + '\'' +
                ", type=" + type +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", source='" + sourceAccountNumber + '\'' +
                ", dest='" + destinationAccountNumber + '\'' +
                ", status=" + status +
                ", desc='" + description + '\'' +
                '}';
    }
}
