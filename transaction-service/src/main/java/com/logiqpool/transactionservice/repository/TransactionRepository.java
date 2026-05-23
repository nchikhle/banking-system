package com.logiqpool.transactionservice.repository;

import com.logiqpool.transactionservice.model.Transaction;
import com.logiqpool.transactionservice.model.TransactionStatus;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    //List<Transaction> findAllByTransactionStatusInAndUpdatedAtBefore(List<TransactionStatus> txStatusList, LocalDateTime threshold);

   /* @Query("SELECT t FROM Transaction t WHERE " +
            "(t.transactionStatus = com.logiqpool.transactionservice.model.TransactionStatus.PENDING AND t.updatedAt <= :threshold) OR " +
            "(t.transactionStatus = com.logiqpool.transactionservice.model.TransactionStatus.FAILED AND t.remarks LIKE 'CRITICAL%' AND t.updatedAt <= :threshold)")
    List<Transaction> findTransactionsToReconcile(@Param("threshold") LocalDateTime threshold);*/


    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.transactionStatus = :pendingStatus AND t.updatedAt <= :threshold) OR " +
            "(t.transactionStatus = :failedStatus AND t.remarks LIKE 'CRITICAL%' AND t.updatedAt <= :threshold)")
    List<Transaction> findTransactionsToReconcile(
            @Param("threshold") LocalDateTime threshold,
            @Param("pendingStatus") TransactionStatus pendingStatus,
            @Param("failedStatus") TransactionStatus failedStatus
    );

    // 💡 Create a default wrapper method so your Scheduler doesn't have to pass the enums manually every time!
    /*default List<Transaction> findTransactionsToReconcile(LocalDateTime threshold) {
        return findTransactionsToReconcile(threshold, TransactionStatus.PENDING, TransactionStatus.FAILED);
    }*/

    /*


    List<Transaction> findByTransactionStatusAndUpdatedAtBeforeOrTransactionStatusAndRemarksStartingWithAndUpdatedAtBefore(
            TransactionStatus status1, LocalDateTime threshold1,
            TransactionStatus status2, String remarkPrefix, LocalDateTime threshold2
    );

    // 💡 Wrap it in a clean default method so your service layer stays beautiful
    default List<Transaction> findTransactionsToReconcile(LocalDateTime threshold) {
        return findByTransactionStatusAndUpdatedAtBeforeOrTransactionStatusAndRemarksStartingWithAndUpdatedAtBefore(
                TransactionStatus.PENDING, threshold,
                TransactionStatus.FAILED, "CRITICAL", threshold
        );
    }


     */
}
