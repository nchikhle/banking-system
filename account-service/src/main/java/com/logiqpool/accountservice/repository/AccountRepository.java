package com.logiqpool.accountservice.repository;

import com.logiqpool.accountservice.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository  extends JpaRepository<Account , UUID> {
    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
        // Check if we've seen this TX-ID before
    boolean existsByLastProcessedTxId(String key);

    // Atomic update to prevent race conditions
    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance - :amount " +
            "WHERE a.accountNumber = :accNum AND a.balance >= :amount")
    int subtractBalanceIfPossible(String accNum, BigDecimal amount);

    @Modifying
    @Transactional
    @Query("UPDATE Account a SET a.balance = a.balance + :amount WHERE a.accountNumber = :accNum")
    int addBalance( String accNum, BigDecimal amount);

    // Update the "Memory" of the account
    @Modifying
    @Transactional
    @Query("UPDATE Account a SET a.lastProcessedTxId = :key WHERE a.accountNumber = :accNum")
    void updateLastTxId(String accNum, String key);


    /*Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByLastProcessedTxId(String key);

    @Modifying
    @Transactional
    @Query("UPDATE Account a SET a.balance = a.balance - :amount " +
            "WHERE a.accountNumber = :accNum AND a.balance >= :amount")
    int subtractBalanceIfPossible(@Param("accNum") String accNum, @Param("amount") BigDecimal amount);

    @Modifying
    @Transactional
    @Query("UPDATE Account a SET a.balance = a.balance + :amount WHERE a.accountNumber = :accNum")
    int addBalance(@Param("accNum") String accNum, @Param("amount") BigDecimal amount);

    @Modifying
    @Transactional
    @Query("UPDATE Account a SET a.lastProcessedTxId = :key WHERE a.accountNumber = :accNum")
    void updateLastTxId(@Param("accNum") String accNum, @Param("key") String key); // 🎯 FIX: Parameters perfectly aligned with JPQL named variables*/
}
