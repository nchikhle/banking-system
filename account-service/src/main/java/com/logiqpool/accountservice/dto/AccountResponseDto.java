package com.logiqpool.accountservice.dto;

import lombok.*;
import com.logiqpool.accountservice.model.AccountType;

import java.math.BigDecimal;

@Setter @Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AccountResponseDto {
    private String accountHolderName;
    private BigDecimal balance;
    private String currency;
    private AccountType accountType;

}
