package com.logiqpool.transactionservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String fromAccount ;

    @Column(unique = true, nullable = false)
    private String toAccount ;

    private TransactionStatus transactionStatus;

    @Column(precision = 4, length = 19)
    private BigDecimal amount;

    private LocalDateTime timestamp;

}
