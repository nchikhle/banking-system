package com.logiqpool.transactionservice.scheduler;

import com.logiqpool.transactionservice.client.AccountClient;
import com.logiqpool.transactionservice.model.Transaction;
import com.logiqpool.transactionservice.model.TransactionStatus;
import com.logiqpool.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionCleanupScheduler {
    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    @Scheduled(fixedDelay = 600000) // Runs every 10 minutes
    @Transactional
    public void reconcile() {
        log.info("Executing Distributed Audit Reconciliation Loop...");
        // Find transactions that stayed PENDING (System crash) or FAILED (Partial failure/Network error)

        // Define the "cutoff" point (e.g., 5 minutes ago)
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        // Fetch both stranded PENDING rows and unresolved CRITICAL FAILED rows
        List<Transaction> problematicTxs = transactionRepository.findTransactionsToReconcile(
                threshold,
                TransactionStatus.PENDING,
                TransactionStatus.FAILED
        );

        if (problematicTxs.isEmpty()) {
            return;
        }

        log.info("Found {} transactions requiring alignment. Healing starting...", problematicTxs.size());

        for (Transaction tx : problematicTxs) {
            String baseKey = "TX-" + tx.getId().toString();
            try {
                // Step 1: Query the Account Service for the specific keys
                boolean debitSuccess = accountClient.checkTransactionStatus(baseKey + "-DEBIT");
                boolean creditSuccess = accountClient.checkTransactionStatus(baseKey + "-CREDIT");
                boolean refundSuccess = accountClient.checkTransactionStatus(baseKey + "-REFUND");

                // Step 2: Determine reality vs local database
                if (creditSuccess) {
                    // Case 1: Everything actually finished, but our DB didn't update
                    tx.setTransactionStatus(TransactionStatus.SUCCESS);
                    tx.setRemarks("Recovered: Credit verified externally.");
                }
                else if (refundSuccess) {
                    // Case 2: The refund happened, we just didn't record the failure
                    tx.setTransactionStatus(TransactionStatus.FAILED);
                    tx.setRemarks("Recovered: Refund verified externally.");
                }
                else if (debitSuccess) {
                    // Case 3: CRITICAL - Money was taken but never credited or refunded
                    // 🎯 This catches your exact scenario!
                    // If the original refund failed, the scheduler retries it using the exact same token.
                    log.warn("TX {} is stuck in Half-Debit state. Retrying auto-refund.", tx.getId());
                    accountClient.updateBalance(tx.getFromAccount(), tx.getAmount(), baseKey + "-REFUND");

                    tx.setTransactionStatus(TransactionStatus.FAILED);
                    tx.setRemarks("Recovered: Auto-refunded by Scheduler.");
                }
                else {
                    // Case 4: Nothing ever happened on the Account side
                    tx.setTransactionStatus(TransactionStatus.FAILED);
                    tx.setRemarks("Recovered: No external activity found.");
                }

                // Step 3: Save the final truth
                // Save the updated, clean status back to the DB
                transactionRepository.save(tx);
                log.info("Successfully reconciled TX: {}", tx.getId());

            } catch (Exception e) {
                log.error("Failed to reconcile TX: {}. Error: {}", tx.getId(), e.getMessage());
                // We don't throw here; let the next loop iteration or next run try again
            }

        }
    }
}