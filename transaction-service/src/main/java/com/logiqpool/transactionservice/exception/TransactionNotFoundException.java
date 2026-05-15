package com.logiqpool.transactionservice.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String id) {
        super(String.format("Transaction with ID %s not found", id));
    }
}