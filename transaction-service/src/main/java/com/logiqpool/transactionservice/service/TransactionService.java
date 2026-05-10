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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor // Lombok: generates the constructor for the private finals
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient; // Your Feign Client
    // private final OutboxRepository outboxRepository;

    @Transactional // CRITICAL: Ensures both the Transaction and Outbox save or both fail
    public void processTransfer(TransferRequest request) {
        
        // 1. VALIDATE: Call Account Service via Feign
        AccountResponseDto fromAccount = accountClient.getAccount(request.getFromAccountNumber());
        log.info("fromAccount: {}",fromAccount);
        log.info("fromAccount.getAccountHolderName(): {}",fromAccount.getAccountHolderName());
        /* if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Sender does not have enough balance");
        }

        // 2. RECORD: Save the transaction as PENDING
        Transaction transaction = new Transaction();
        transaction.setFromAccount(request.getFromAccountNumber());
        transaction.setToAccount(request.getToAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setTransactionStatus(TransactionStatus.valueOf("PENDING"));
        transactionRepository.save(transaction);

        // 3. OUTBOX: Save the event that needs to go to Kafka
        // This is the "Outbox Pattern"
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(transaction.getId().toString());
        event.setType("TRANSFER_INITIATED");
        event.setPayload(serializeToJson(request)); // Helper to convert DTO to String
        outboxRepository.save(event);*/
        
        // When this method ends, @Transactional commits both to the DB.
    }
}