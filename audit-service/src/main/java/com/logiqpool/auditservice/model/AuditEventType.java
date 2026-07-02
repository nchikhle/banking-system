package com.logiqpool.auditservice.model;

public enum AuditEventType {

    ACCOUNT_CREATED,
    ACCOUNT_UPDATED,

    TRANSFER_STARTED,

    DEBIT_SUCCESS,
    DEBIT_FAILED,

    CREDIT_SUCCESS,
    CREDIT_FAILED,

    REFUND_SUCCESS,
    REFUND_FAILED,

    SCHEDULER_RECOVERY,

    LOGIN_SUCCESS,
    LOGIN_FAILED
}