package com.logiqpool.transactionservice.scheduler;

import com.logiqpool.transactionservice.client.AccountClient;
import com.logiqpool.transactionservice.dto.BalanceChangeRequestDto;
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

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    @Scheduled(fixedDelay = 600000) // Runs every 10 minutes
    public void reconcile() {
        log.info("Executing distributed audit reconciliation loop...");

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        List<Transaction> problematicTxs =
                transactionRepository.findTransactionsToReconcile(threshold);

        if (problematicTxs.isEmpty()) {
            log.info("No transactions found for reconciliation.");
            return;
        }

        log.info("Found {} transactions requiring reconciliation.", problematicTxs.size());

        for (Transaction tx : problematicTxs) {
            reconcileTransaction(tx);
        }
    }

    private void reconcileTransaction(Transaction tx) {
        String baseKey = "TX-" + tx.getId();

        String debitKey = baseKey + "-DEBIT";
        String creditKey = baseKey + "-CREDIT";
        String refundKey = baseKey + "-REFUND";

        try {
            boolean debitSuccess = accountClient.checkTransactionStatus(debitKey).processed();
            boolean creditSuccess = accountClient.checkTransactionStatus(creditKey).processed();
            boolean refundSuccess = accountClient.checkTransactionStatus(refundKey).processed();

            if (creditSuccess) {
                tx.setTransactionStatus(TransactionStatus.SUCCESS);
                tx.setRemarks("Recovered: Credit verified externally.");

            } else if (refundSuccess) {
                tx.setTransactionStatus(TransactionStatus.FAILED);
                tx.setRemarks("Recovered: Refund verified externally.");

            } else if (debitSuccess) {
                log.warn("TX {} is stuck after debit. Retrying refund.", tx.getId());

                accountClient.updateBalance(
                        tx.getFromAccount(),
                        refundKey,
                        BalanceChangeRequestDto.builder()
                                .amount(tx.getAmount())
                                .build()
                );

                tx.setTransactionStatus(TransactionStatus.FAILED);
                //tx.setRemarks("Recovered: Auto-refunded by scheduler.");
                tx.setRemarks("Recovered: Debit was verified, credit/refund missing, auto-refunded by scheduler.");

            } else {
                tx.setTransactionStatus(TransactionStatus.FAILED);
                tx.setRemarks("Recovered: No external account activity found.");
            }

            transactionRepository.save(tx);

            log.info("Successfully reconciled TX: {}", tx.getId());

        } catch (Exception e) {
            log.error(
                    "Failed to reconcile TX {}. It will be retried in the next scheduler run. Error: {}",
                    tx.getId(),
                    e.getMessage(),
                    e
            );
        }
    }
}