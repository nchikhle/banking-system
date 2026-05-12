package com.logiqpool.transactionservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)

@Table(name = "transactions", uniqueConstraints = {
        // Prevents the same business request from being saved twice
        @UniqueConstraint(name = "uk_transaction_reference", columnNames = {"transaction_reference"})
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String fromAccount ;
    private String toAccount ;

    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus; // PENDING, SUCCESS, FAILED

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "transaction_reference", nullable = false)
    private String transactionReference; // Unique ref for tracking

    @CreatedDate // Automatically fills on "insert"
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // When it last moved

    private String failureReason;
}
