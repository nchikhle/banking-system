package com.logiqpool.accountservice.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String s) {
        super(s);
    }
}