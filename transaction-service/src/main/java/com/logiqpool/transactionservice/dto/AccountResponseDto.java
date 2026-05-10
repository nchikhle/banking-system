package com.logiqpool.transactionservice.dto;

import com.logiqpool.transactionservice.model.AccountType;
import lombok.Builder;

import java.math.BigDecimal;


@Builder // You can still keep @Builder for easy object creation
public record AccountResponseDto(
    String accountNumber,
    String accountHolderName,
    BigDecimal balance,
    String currency,
    AccountType accountType
){}

