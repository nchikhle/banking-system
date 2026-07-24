package com.logiqpool.transactionservice.dto;

import com.logiqpool.transactionservice.model.AuditEventType;
import com.logiqpool.transactionservice.model.AuditStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record AuditEventResponseDto(

        UUID id,

        String correlationId,

        String transactionReference,

        String accountNumber,

        String serviceName,

        AuditEventType eventType,

        AuditStatus status,

        String remarks,

        LocalDateTime createdAt
) {
}