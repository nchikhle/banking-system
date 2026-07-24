package com.logiqpool.accountservice.dto;

import com.logiqpool.accountservice.model.AuditEventType;
import com.logiqpool.accountservice.model.AuditStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AuditEventRequestDto(

        @NotBlank
        String correlationId,

        @NotBlank
        String transactionReference,

        String accountNumber,

        @NotBlank
        String serviceName,

        @NotNull
        AuditEventType eventType,

        @NotNull
        AuditStatus status,

        String remarks
) {
}