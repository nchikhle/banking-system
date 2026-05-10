package com.logiqpool.transactionservice.dto;

import com.logiqpool.transactionservice.model.AccountType;

import lombok.*;

import java.math.BigDecimal;

@Setter @Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AccountResponseDto {
    private String accountHolderName;
    private BigDecimal balance;
    private String currency;
    private AccountType accountType;

}
