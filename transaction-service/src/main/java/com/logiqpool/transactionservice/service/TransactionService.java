package com.logiqpool.transactionservice.service;

import com.logiqpool.transactionservice.client.AccountClient;
import com.logiqpool.transactionservice.dto.TransferRequest;
import com.logiqpool.transactionservice.exception.AccountServiceIntegrationException;
import com.logiqpool.transactionservice.exception.InvalidTransactionException;
import com.logiqpool.transactionservice.model.Transaction;
import com.logiqpool.transactionservice.model.TransactionStatus;
import com.logiqpool.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor // Lombok: generates the constructor for the private finals
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient; // Your Feign Client
    private final TransactionInternalService internalService;

    /**
     * Note: @Transactional is omitted here intentionally.
     * We want internalService to commit statuses to the DB immediately
     * regardless of network timeouts in Feign calls.
     */

    // @Transactional // CRITICAL: Ensures both the Transaction and Outbox save or both fail
    public void processTransfer(TransferRequest request) {

        // Business Rule validation check before touching the DB
        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            throw new InvalidTransactionException("Source and destination account numbers cannot be identical.");
        }
        if (request.amount().signum() <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero.");
        }

        // 1. Initialize PENDING record (Handled in its own TX via REQUIRES_NEW)
        // This is handled by your InternalService to ensure a fresh transaction context
        // Save intent to DB (PENDING status + Unique Reference)
        Transaction tx = internalService.startTransaction(request);

        // This key ensures Account Service doesn't double-charge
        String idempotencyKey = "TX-" + tx.getId();

        try {
            // 2. THE MONEY MOVE (Network calls to Account Service)
            log.info("Starting transfer for TX: {}", tx.getId());

            try {
                // STEP A: DEBIT
                accountClient.updateBalance(
                        request.fromAccountNumber(),
                        request.amount().negate(),
                        idempotencyKey + "-DEBIT"
                );

                try {
                    // STEP B: CREDIT
                    // For testing failure, uncomment the line below:
                    // if(true) throw new RuntimeException("Network Timeout during Credit!");

                    accountClient.updateBalance(
                            request.toAccountNumber(),
                            request.amount(),
                            idempotencyKey + "-CREDIT"
                    );

                    // 3. SUCCESS: Finalize the transaction
                    internalService.finalizeStatus(tx.getId(), TransactionStatus.SUCCESS, "Completed successfully");
                    log.info("Transfer completed successfully: {}", tx.getId());

                } catch (AccountServiceIntegrationException e) {
                    log.error("Credit step failed for TX: {}, initiating compensation refund. Reason: {}", tx.getId(), e.getMessage());

                    // STEP C: COMPENSATION (The Refund)
                    // We use a specific suffix so the Account Service knows this is a refund
                    try {
                        accountClient.updateBalance(
                                request.fromAccountNumber(),
                                request.amount(),
                                idempotencyKey + "-REFUND"
                        );

                        internalService.finalizeStatus(tx.getId(), TransactionStatus.FAILED, "Credit failed - Money Refunded");
                    } catch (Exception refundError) {
                        // CRITICAL: The Debit happened, the Credit failed, AND the Refund failed.
                        log.error("CRITICAL DATA INCONSISTENCY: Refund failed for TX: {}! Manual intervention required.", tx.getId(), refundError);
                        internalService.finalizeStatus(tx.getId(), TransactionStatus.FAILED, "CRITICAL: Refund failed. System out of sync.");
                    }
                    throw e; // Rethrow integration exception for Controller handling
                }

            //if(true) throw new RuntimeException("Network Timeout - Transaction service is failed!");
            } catch (AccountServiceIntegrationException e) {
                // Initial DEBIT failed (e.g., 404 Account Not Found or 422 Insufficient Funds)
                // If the initial DEBIT failed, we just mark the whole thing as FAILED.
                // No refund is needed because no money ever moved.

                log.error("Debit step failed for TX {}: {}", tx.getId(), e.getMessage());
                internalService.finalizeStatus(tx.getId(), TransactionStatus.FAILED, e.getMessage());
                throw e;
            }
        } catch(Exception e) {
            //log.error("Transfer process terminated for {}: {}", tx.getId(), e.getMessage());
            // Keep ONLY this catch-all block to safeguard against random infrastructure crashes
            // (like Postgres dropping its connection mid-method execution)
            log.error("Unexpected internal infrastructure failure during processing for TX {}: {}", tx.getId(), e.getMessage(), e);
            internalService.finalizeStatus(tx.getId(), TransactionStatus.FAILED, "System error: " + e.getMessage());
            throw new AccountServiceIntegrationException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected processing failure: " + e.getMessage());
        }
    }
}