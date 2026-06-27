package com.logiqpool.transactionservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

/***
 *
 * @param amount
 *
 * But here, amount can be positive or negative:
 *
 * positive amount = deposit
 * negative amount = withdrawal
 *
 * So do not use:
 *
 * @DecimalMin("0.01")
 *
 * because withdrawal needs negative amount.
 *
 * Instead, validate zero manually in service:
 *
 * if (amount.compareTo(BigDecimal.ZERO) == 0) {
 *     throw new IllegalArgumentException("Amount must be non-zero");
 * }
 */
@Builder
public record BalanceChangeRequestDto(

        @NotNull(message = "Amount is required")
        BigDecimal amount

) {
}