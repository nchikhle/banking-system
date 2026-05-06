package com.logiqpool.accountservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Setter @Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AccountResponseDto {
    private String accountHolderName;
    private BigDecimal balance;
    private String currency;

}
