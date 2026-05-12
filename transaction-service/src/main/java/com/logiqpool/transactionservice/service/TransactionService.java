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

    // @Transactional // CRITICAL: Ensures both the Transaction and Outbox save or both fail
    public void processTransfer(TransferRequest request) {

        // This is handled by your InternalService to ensure a fresh transaction context
        // STEP 1: Save intent to DB (PENDING status + Unique Reference) (Handled in its own TX via REQUIRES_NEW)
        Transaction tx = internalService.startTransaction(request);

        // This key ensures Account Service doesn't double-charge
        String idempotencyKey = "TX-" + tx.getId();

        try {
            // 1. VALIDATE: Call Account Service via Feign
            AccountResponseDto fromAccount = accountClient.getAccount(request.fromAccountNumber());
            BigDecimal amount = request.amount();
            log.info("fromAccount: {}", fromAccount);
            //log.info("fromAccount.getAccountHolderName(): {}", fromAccount.accountHolderName());

            if (fromAccount.balance().compareTo(amount) < 0) {
                internalService.finalizeStatus(tx.getId(), TransactionStatus.FAILED, "Insufficient Funds");
                throw new RuntimeException("Insufficient Funds");
            }

            // 2. THE MONEY MOVE (Network calls to Account Service)
            try {
                // DEBIT step
                // Now the Account Service can see this key and handle it
                accountClient.updateBalance(fromAccount.accountNumber(), amount.negate(), idempotencyKey+ "-DEBIT"); // Debit

                try {
                    // CREDIT step
                    //if(true) throw new RuntimeException("Network Timeout - Credit Failed!");
                    accountClient.updateBalance(request.toAccountNumber(), amount, idempotencyKey+ "-CREDIT"); //Credit
                } catch (Exception e) {
                    log.error("Credit failed for {}, initiating refund", tx.getId());

                    // COMPENSATING TRANSACTION (The Refund)
                    // We use a specific suffix so the Account Service knows this is a refund
                    accountClient.updateBalance(fromAccount.accountNumber(), amount, idempotencyKey+ "-REFUND"); // Credit back

                    //finalizeTransaction(tx, TransactionStatus.FAILED, "Credit failed - Money Refunded");
                    internalService.finalizeStatus(tx.getId(), TransactionStatus.FAILED, "Credit failed - Money Refunded");
                    // throw new TransferFailedException("System error, money returned.");
                    throw new RuntimeException("System error during credit, money returned.");
                }
            } catch (Exception e) {
                // Handle global failure
                // If the Debit itself failed or the refund logic threw an error
                log.error("Debit/Transfer failed: {}", e.getMessage());
                internalService.finalizeStatus(tx.getId(), TransactionStatus.FAILED, e.getMessage());
                throw e;
            }

            // 3. SUCCESS: Update the original transaction record
            // 3. SUCCESS
            internalService.finalizeStatus(tx.getId(), TransactionStatus.SUCCESS, "Completed successfully");
            // TODO: logAudit(tx, "SUCCESS", "Transfer completed");

        } catch (Exception e) {
            log.error("Transfer process terminated for {}: {}", tx.getId(), e.getMessage());
            throw e;
        }
    }
}