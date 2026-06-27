package com.logiqpool.transactionservice.dto;

import lombok.Builder;

@Builder
public record TransactionStatusResponseDto(
        String idempotencyKey,
        boolean processed
) {
}