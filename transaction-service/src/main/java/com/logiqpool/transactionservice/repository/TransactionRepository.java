package com.logiqpool.transactionservice.repository;

import com.logiqpool.transactionservice.model.Transaction;
import com.logiqpool.transactionservice.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("""
        SELECT t
        FROM Transaction t
        WHERE
            (t.transactionStatus = :pendingStatus AND t.updatedAt <= :threshold)
            OR
            (t.transactionStatus = :failedStatus
                AND t.remarks LIKE 'CRITICAL%'
                AND t.updatedAt <= :threshold)
    """)
    List<Transaction> findTransactionsToReconcile(
            @Param("threshold") LocalDateTime threshold,
            @Param("pendingStatus") TransactionStatus pendingStatus,
            @Param("failedStatus") TransactionStatus failedStatus
    );

    default List<Transaction> findTransactionsToReconcile(LocalDateTime threshold) {
        return findTransactionsToReconcile(
                threshold,
                TransactionStatus.PENDING,
                TransactionStatus.FAILED
        );
    }
}