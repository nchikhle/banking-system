package com.logiqpool.transactionservice.dto;

import com.logiqpool.transactionservice.model.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
@Builder // Useful for creating objects in tests
public record TransferRequest(
        String fromAccountNumber,
        String toAccountNumber,
        TransactionStatus transactionStatus,
        BigDecimal amount){ }
