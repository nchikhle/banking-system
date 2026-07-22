package com.logiqpool.auditservice.service;

import com.logiqpool.auditservice.dto.AuditEventRequestDto;
import com.logiqpool.auditservice.dto.AuditEventResponseDto;
import com.logiqpool.auditservice.model.AuditEvent;
import com.logiqpool.auditservice.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    public final AuditRepository auditRepository;
    public AuditEventResponseDto createAuditEvent(AuditEventRequestDto request) {
        log.info("Creating audit event: {}", request.eventType());
        AuditEvent event = AuditEvent.builder()
                .correlationId(request.correlationId())
                .transactionReference(request.transactionReference())
                .accountNumber(request.accountNumber())
                .serviceName(request.serviceName())
                .eventType(request.eventType())
                .status(request.status())
                .remarks(request.remarks())
                .build();
        AuditEvent saved = auditRepository.save(event);

        return map(saved);
    }

    private AuditEventResponseDto map(AuditEvent event) {

        return AuditEventResponseDto.builder()
                .id(event.getId())
                .correlationId(event.getCorrelationId())
                .transactionReference(event.getTransactionReference())
                .accountNumber(event.getAccountNumber())
                .serviceName(event.getServiceName())
                .eventType(event.getEventType())
                .status(event.getStatus())
                .remarks(event.getRemarks())
                .createdAt(event.getCreatedAt())
                .build();
    }

}
