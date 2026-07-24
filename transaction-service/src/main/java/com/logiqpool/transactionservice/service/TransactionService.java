package com.logiqpool.transactionservice.service;

import com.logiqpool.transactionservice.client.AccountClient;
import com.logiqpool.transactionservice.client.AuditClient;
import com.logiqpool.transactionservice.dto.AuditEventRequestDto;
import com.logiqpool.transactionservice.dto.BalanceChangeRequestDto;
import com.logiqpool.transactionservice.dto.TransferRequest;
import com.logiqpool.transactionservice.exception.AccountServiceIntegrationException;
import com.logiqpool.transactionservice.exception.InvalidTransactionException;
import com.logiqpool.transactionservice.model.AuditEventType;
import com.logiqpool.transactionservice.model.AuditStatus;
import com.logiqpool.transactionservice.model.Transaction;
import com.logiqpool.transactionservice.model.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor // Lombok: generates the constructor for the private finals
public class TransactionService {

    private final AccountClient accountClient; // Your Feign Client
    private final AuditClient auditClient;
    private final TransactionInternalService internalService;

    /**
     * Note: @Transactional is omitted here intentionally.
     * We want internalService to commit statuses to the DB immediately
     * regardless of network timeouts in Feign calls.
     */


    /**
     * Intentionally not annotated with @Transactional.
     *
     * This method performs remote calls to Account Service. Keeping a DB transaction
     * open across network calls can create long-running transactions and rollback confusion.
     * Transaction state is persisted through TransactionInternalService using smaller
     * independent transactions.
     */


    public void processTransfer(TransferRequest request) {

        validateTransferRequest(request);

        // 1. Initialize PENDING record (Handled in its own TX via REQUIRES_NEW)
        // This is handled by your InternalService to ensure a fresh transaction context
        // Save intent to DB (PENDING status + Unique Reference)
        Transaction tx = internalService.startTransaction(request);

        // This key ensures Account Service doesn't double-charge
        String baseIdempotencyKey = "TX-" + tx.getId();

        auditClient.createAuditEvent(
                AuditEventRequestDto.builder()
                        .correlationId(baseIdempotencyKey)
                        .transactionReference(request.transactionReference())
                        .accountNumber(request.fromAccountNumber())
                        .serviceName("transaction-service")
                        .eventType(AuditEventType.TRANSFER_STARTED)
                        .status(AuditStatus.INFO)
                        .remarks(
                                "Transfer started from "
                                        + request.fromAccountNumber()
                                        + " to "
                                        + request.toAccountNumber()
                        )
                        .build()
        );

        try {
            // 2. THE MONEY MOVE (Network calls to Account Service)

            log.info("Starting transfer execution sequence for TX: {}", tx.getId());

            // STEP A: DEBIT
            debitSourceAccount(request, tx, baseIdempotencyKey);

            // STEP B: CREDIT
            creditDestinationAccount(request, tx, baseIdempotencyKey);

            // 3. SUCCESS: Finalize the transaction
            internalService.finalizeStatus(
                    tx.getId(),
                    TransactionStatus.SUCCESS,
                    "Completed successfully"
            );
            auditClient.createAuditEvent(
                    AuditEventRequestDto.builder()
                            .correlationId(baseIdempotencyKey)
                            .transactionReference(request.transactionReference())
                            .accountNumber(request.fromAccountNumber())
                            .serviceName("transaction-service")
                            .eventType(AuditEventType.TRANSFER_SUCCESS)
                            .status(AuditStatus.SUCCESS)
                            .remarks(
                                    "Money Transfer from "
                                            + request.fromAccountNumber()
                                            + " to "
                                            + request.toAccountNumber()
                                            + " Completed successfully"
                            )
                            .build()
            );

            log.info("Transfer completed successfully: {}", tx.getId());

            //if(true) throw new RuntimeException("Network Timeout - Transaction service is failed!");
        } catch (AccountServiceIntegrationException | InvalidTransactionException e) {
            throw e;

        } catch(Exception e) {
            /* // 🎯 FIX: If it's a known business/integration exception, pass it through without touching DB state
           if (e instanceof AccountServiceIntegrationException || e instanceof InvalidTransactionException) {
                throw e;
            }*/

            // Keep ONLY this catch-all block to safeguard against random infrastructure crashes
            // such as Postgres dropping its connection mid-method execution
            // Fallback safeguards ONLY against random core infrastructure failures (e.g. Postgres pool death mid-execution)

            log.error("Transfer process terminated:: Unexpected internal infrastructure failure during processing for TX {}: {}",
                    tx.getId(),
                    e.getMessage(),
                    e);

            internalService.finalizeStatus(
                    tx.getId(),
                    TransactionStatus.FAILED,
                    "System error: " + e.getMessage());

            auditClient.createAuditEvent(
                    AuditEventRequestDto.builder()
                            .correlationId(baseIdempotencyKey)
                            .transactionReference(request.transactionReference())
                            .accountNumber(request.fromAccountNumber())
                            .serviceName("transaction-service")
                            .eventType(AuditEventType.TRANSFER_FAILED)
                            .status(AuditStatus.FAILED)
                            .remarks(
                                    "System error: " + e.getMessage()
                            )
                            .build()
            );

            throw new AccountServiceIntegrationException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unexpected processing failure: " + e.getMessage()
            );
        }
    }

    private void debitSourceAccount(TransferRequest request, Transaction tx, String baseIdempotencyKey) {
        try {

            accountClient.updateBalance(
                    request.fromAccountNumber(),
                    baseIdempotencyKey + "-DEBIT",
                    BalanceChangeRequestDto.builder()
                            .amount(request.amount().negate())
                            .build()

            );
            log.info("Debit completed for TX: {}", tx.getId());

        } catch (Exception e) { // 🎯 FIX: Catch all exceptions to avoid missing errors
            // Initial DEBIT failed (e.g., 404 Account Not Found or 422 Insufficient Funds)
            // If the initial DEBIT failed, we just mark the whole thing as FAILED.
            // No refund is needed because no money ever moved.

            log.error("Debit step failed for TX {}: {}", tx.getId(), e.getMessage(), e);
            internalService.finalizeStatus(
                    tx.getId(),
                    TransactionStatus.FAILED,
                    "Debit failed or unknown: " + e.getMessage()
            );

            throw e;
        }
    }

    private void creditDestinationAccount(TransferRequest request, Transaction tx, String baseIdempotencyKey) {
        try {
            // For testing failure, uncomment the line below:
            // if(true) throw new RuntimeException("Network Timeout during Credit!");

            accountClient.updateBalance(
                    request.toAccountNumber(),
                    baseIdempotencyKey + "-CREDIT",
                    BalanceChangeRequestDto.builder()
                            .amount(request.amount())
                            .build()
            );

            log.info("Credit completed for TX: {}", tx.getId());

        } catch (Exception e) { // 🎯 FIX: Catch all exceptions to guarantee compensation runs
            // STEP C: COMPENSATION (The Refund)
            // We use a specific suffix so the Account Service knows this is a refund

            log.error("Credit step failed for TX {}: {}" , tx.getId(), e.getMessage(), e);

            log.info("initiating compensation refund profile. Reason: {}", tx.getId());

            handleImmediateRefund(request, baseIdempotencyKey, tx);

            throw e; // Rethrow integration exception for Controller handling
        }
    }


    private void handleImmediateRefund(TransferRequest request, String idempotencyKey, Transaction tx) {
        try {
            accountClient.updateBalance(
                    request.fromAccountNumber(),
                    idempotencyKey + "-REFUND",
                    BalanceChangeRequestDto.builder()
                            .amount(request.amount())
                            .build()
            );

            internalService.finalizeStatus(
                    tx.getId(),
                    TransactionStatus.SUCCESS,
                    "Credit failed - Money Refunded"
            );

            auditClient.createAuditEvent(
                    AuditEventRequestDto.builder()
                            .correlationId("TX-" + tx.getId())
                            .transactionReference(request.transactionReference())
                            .accountNumber(request.fromAccountNumber())
                            .serviceName("transaction-service")
                            .eventType(AuditEventType.REFUND_SUCCESS)
                            .status(AuditStatus.SUCCESS)
                            .remarks("Credit failed - Money Refunded")
                            .build()
            );

            log.info("Refund completed successfully for TX: {}", tx.getId());

        } catch (Exception refundError) {
            // CRITICAL: The Debit happened, the Credit failed, AND the Refund failed.

            log.error("CRITICAL DATA INCONSISTENCY: Refund failed for TX: {}! " +
                    "Manual intervention required.",
                    tx.getId(),
                    refundError
            );

            internalService.finalizeStatus(
                    tx.getId(),
                    TransactionStatus.FAILED,
                    "CRITICAL: Refund failed. System out of sync."
            );

            auditClient.createAuditEvent(
                    AuditEventRequestDto.builder()
                            .correlationId("TX-" + tx.getId())
                            .transactionReference(request.transactionReference())
                            .accountNumber(request.fromAccountNumber())
                            .serviceName("transaction-service")
                            .eventType(AuditEventType.REFUND_FAILED)
                            .status(AuditStatus.FAILED)
                            .remarks("CRITICAL: Refund failed. System out of sync.")
                            .build()
            );

        }

    }
    private void validateTransferRequest(TransferRequest request) {
        if (request == null) {
            throw new InvalidTransactionException("Transfer request cannot be null.");
        }

        if (request.fromAccountNumber() == null || request.fromAccountNumber().isBlank()) {
            throw new InvalidTransactionException("Source account number is required.");
        }

        if (request.toAccountNumber() == null || request.toAccountNumber().isBlank()) {
            throw new InvalidTransactionException("Destination account number is required.");
        }

        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            throw new InvalidTransactionException("Source and destination account numbers cannot be identical.");
        }

        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero.");
        }
    }
}