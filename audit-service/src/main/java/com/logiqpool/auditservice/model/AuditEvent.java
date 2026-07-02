package com.logiqpool.auditservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Common identifier shared across services.
     * Example:
     * TX-7c5f3b25
     */
    @Column(nullable = false)
    private String correlationId;

    /**
     * Business transaction reference.
     */
    @Column(nullable = false)
    private String transactionReference;

    /**
     * Account involved in this event.
     */
    private String accountNumber;

    /**
     * Which microservice generated this event.
     * Example:
     * account-service
     * transaction-service
     * scheduler
     */
    @Column(nullable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditStatus status;

    @Column(length = 1000)
    private String remarks;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}