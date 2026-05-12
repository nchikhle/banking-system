package com.logiqpool.transactionservice.scheduler;

import com.logiqpool.transactionservice.client.AccountClient;
import com.logiqpool.transactionservice.model.Transaction;
import com.logiqpool.transactionservice.model.TransactionStatus;
import com.logiqpool.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionCleanupScheduler {
    private final TransactionRepository repository;
    private final AccountClient accountClient;

    @Scheduled(fixedDelay = 600000) // Runs every 10 minutes
    public void reconcile() {
        // Find transactions that stayed PENDING (System crash) 
        // or FAILED (Partial failure/Network error)

        // Define the "cutoff" point (e.g., 10 minutes ago)
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);

        // Statuses we are worried about
        List<TransactionStatus> targetStatuses = List.of(
                TransactionStatus.PENDING,
                TransactionStatus.FAILED
        );

        // Call the new repository method
       /* List<Transaction> problematicTxs = repository.findAllByTransactionStatusInAndCreatedAtBefore(
                targetStatuses,
                threshold
        );*/

        List<Transaction> problematicTxs = repository.findAllByTransactionStatusInAndUpdatedAtBefore(
                List.of(TransactionStatus.PENDING, TransactionStatus.FAILED),
                LocalDateTime.now().minusMinutes(10)
        );


        log.info("Found {} transactions requiring reconciliation", problematicTxs.size());

        for (Transaction tx : problematicTxs) {
            // Step 1: Call AccountClient to check the external reality
            // Step 2: Update tx.setTransactionStatus based on the response
            // Step 3: repository.save(tx)

            // Logic: Call AccountService.verify(tx.getId()) 
            // If debited but not credited -> Complete the credit or Refund.
            log.warn("Reconciling inconsistent state for Transaction: {}", tx.getId());
        }
    }
}

/**
 * @Scheduled(fixedDelay = 300000) // Every 5 mins
 * public void reconcileZombies() {
 *     // 1. Find PENDING records older than 10 mins
 *     List<Transaction> zombies = repo.findPendingOlderThan(Duration.ofMinutes(10));
 *
 *     for (Transaction tx : zombies) {
 *         // 2. Cross-reference with the Account Service
 *         AccountStatus actualStatus = accountClient.verifyTransaction(tx.getId());
 *
 *         if (actualStatus.isDebited()) {
 *             tx.setTransactionStatus(TransactionStatus.SUCCESS);
 *         } else {
 *             tx.setTransactionStatus(TransactionStatus.FAILED);
 *             tx.setFailureReason("System timed out - No funds moved");
 *         }
 *         repo.save(tx);
 *     }
 * }
 */