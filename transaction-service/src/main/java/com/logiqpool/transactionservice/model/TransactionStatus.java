package com.logiqpool.transactionservice.model;

public enum TransactionStatus {
        PENDING,
        DEBITED,
        SUCCESS,
        FAILED,
        COMPENSATION_PENDING,
        COMPENSATED,
        COMPENSATION_FAILED,
        COMPLETED
    }