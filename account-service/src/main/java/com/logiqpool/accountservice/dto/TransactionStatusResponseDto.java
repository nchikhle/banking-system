package com.logiqpool.accountservice.dto;

import lombok.Builder;

@Builder
public record TransactionStatusResponseDto(
        String idempotencyKey,
        boolean processed
) {
}