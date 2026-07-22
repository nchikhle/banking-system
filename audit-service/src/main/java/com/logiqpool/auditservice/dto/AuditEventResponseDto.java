package com.logiqpool.auditservice.dto;

import com.logiqpool.auditservice.model.AuditEventType;
import com.logiqpool.auditservice.model.AuditStatus;
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