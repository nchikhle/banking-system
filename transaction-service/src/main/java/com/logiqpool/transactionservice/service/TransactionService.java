package com.logiqpool.transactionservice.service;

import com.logiqpool.transactionservice.client.AccountClient;
import com.logiqpool.transactionservice.dto.AccountResponseDto;
import com.logiqpool.transactionservice.dto.TransferRequest;
import com.logiqpool.transactionservice.model.Transaction;
import com.logiqpool.transactionservice.model.TransactionStatus;
import com.logiqpool.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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

                } catch (Exception e) {
                    log.error("Credit failed for {}, initiating immediate refund", tx.getId());

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
                        log.error("CRITICAL ERROR: Refund failed for {}! Data is inconsistent.", tx.getId());
                        internalService.finalizeStatus(tx.getId(), TransactionStatus.FAILED, "STUCK: Refund failed. Manual check required.");
                    }
                    throw new RuntimeException("Transfer failed during credit step: " + e.getMessage());
                }

            //if(true) throw new RuntimeException("Network Timeout - Transaction service is failed!");
            } catch (Exception e) {
                // If the initial DEBIT failed, we just mark the whole thing as FAILED.
                // No refund is needed because no money ever moved.
                log.error("Debit step failed: {}", e.getMessage());
                internalService.finalizeStatus(tx.getId(), TransactionStatus.FAILED, e.getMessage());
                throw e;
            }
        } catch(Exception e) {
            log.error("Transfer process terminated for {}: {}", tx.getId(), e.getMessage());
            // We re-throw so the Controller can return the appropriate Error Response
            throw e;
        }
    }
}