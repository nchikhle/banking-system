package com.logiqpool.accountservice.repository;

import com.logiqpool.accountservice.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository  extends JpaRepository<Account , UUID> {
    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
    // Check if we've seen this TX-ID before

    /**
     * Demo-only idempotency check.
     * Production should track all idempotency keys in a separate table.
     */
    //boolean existsByLastProcessedTxId(String key);

    /**
     * Pessimistic lock version.
     * Useful when we want to fetch the account, validate in Java,
     * then update safely within the same transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a
        FROM Account a
        WHERE a.accountNumber = :accountNumber
    """)
    Optional<Account> findByAccountNumberWithLock(
            @Param("accountNumber") String accountNumber
    );

    /**
     * Atomic withdrawal.
     * Balance check and balance deduction happen in one SQL update.
     * This prevents race conditions during concurrent withdrawals.
     */
    @Modifying
    @Query("""
    UPDATE Account a
    SET a.balance = a.balance - :amount,
        a.updatedAt = CURRENT_TIMESTAMP
    WHERE a.accountNumber = :accNum
    AND a.balance >= :amount
    """)
    int subtractBalanceIfPossible(
            @Param("accNum") String accNum,
            @Param("amount") BigDecimal amount
    );

    /**
     * Atomic deposit.
     */
    @Modifying
    @Query("""
        UPDATE Account a
        SET a.balance = a.balance + :amount,
            a.updatedAt = CURRENT_TIMESTAMP
        WHERE a.accountNumber = :accNum
    """)
    int addBalance(
            @Param("accNum") String accNum,
            @Param("amount") BigDecimal amount
    );

    /**
     * Simplified idempotency tracking.
     * Production version should use a separate transaction/idempotency table.

    @Modifying
    @Query("""
    UPDATE Account a
    SET a.lastProcessedTxId = :key,
        a.updatedAt = CURRENT_TIMESTAMP
    WHERE a.accountNumber = :accNum
    """)
    int updateLastTxId(
            @Param("accNum") String accNum,
            @Param("key") String key
    ); */
}

/***
 * Should repository methods have @Transactional?
 *
 * You currently have:
 *
 * @Modifying
 * @Transactional
 * @Query(...)
 * int addBalance(...)
 *
 * This works, but I would remove @Transactional from repository methods and keep transaction boundary in the service.
 *
 * You already have:
 *
 * @Transactional
 * public void processBalanceChange(...)
 *
 * That is better because one full business operation stays inside one transaction:
 *
 * 1. Check idempotency
 * 2. Check account exists
 * 3. Update balance
 * 4. Store idempotency key
 *
 * So repository should usually only describe database operations. Service should control transactions.
 *
 * Interview line:
 *
 * I keep @Transactional at service layer because the service method represents the complete business use case. Repository methods only execute queries.
 *
 * Why @Modifying is needed?
 *
 * For SELECT queries, Spring Data JPA knows it is reading data.
 *
 * But these queries modify data:
 *
 * UPDATE Account a SET a.balance = ...
 *
 * So we need:
 *
 * @Modifying
 *
 * Without it, Spring treats the query like a select query and may throw an exception.
 *
 * Interview line:
 *
 * @Modifying tells Spring Data JPA that this JPQL query performs an update or delete operation instead of returning entities.
 *
 * Why atomic update is strong?
 *
 * This query is good:
 *
 * @Query("""
 *     UPDATE Account a
 *     SET a.balance = a.balance - :amount
 *     WHERE a.accountNumber = :accNum
 *     AND a.balance >= :amount
 * """)
 * int subtractBalanceIfPossible(...)
 *
 * Because this avoids the unsafe pattern:
 *
 * Thread 1 reads balance = 100
 * Thread 2 reads balance = 100
 * Thread 1 withdraws 80
 * Thread 2 withdraws 80
 * Final result becomes invalid
 *
 * Your query says:
 *
 * Deduct money only if balance is still enough at update time.
 *
 * If two withdrawals come together, only one will succeed when funds are not enough for both.
 */