package com.logiqpool.transactionservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AccountServiceIntegrationException extends RuntimeException {
    private final HttpStatus statusCode;

    public AccountServiceIntegrationException(HttpStatus statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}