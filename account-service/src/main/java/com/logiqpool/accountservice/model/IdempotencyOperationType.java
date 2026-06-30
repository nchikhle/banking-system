package com.logiqpool.accountservice.model;

public enum IdempotencyOperationType {
    DEBIT,
    CREDIT,
    REFUND,
    UNKNOWN
}