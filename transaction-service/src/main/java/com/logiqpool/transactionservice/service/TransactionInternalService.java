package com.logiqpool.transactionservice.service;

import com.logiqpool.transactionservice.dto.TransferRequest;
import com.logiqpool.transactionservice.model.Transaction;
import com.logiqpool.transactionservice.model.TransactionStatus;
import com.logiqpool.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionInternalService {
    private final TransactionRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction startTransaction(TransferRequest req) {
        Transaction tx = new Transaction();
        tx.setTransactionReference(req.transactionReference());
        tx.setFromAccount(req.fromAccountNumber());
        tx.setToAccount(req.toAccountNumber());
        tx.setAmount(req.amount());
        tx.setTransactionStatus(TransactionStatus.PENDING);
        return repository.save(tx);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeStatus(UUID id, TransactionStatus status, String reason) {
        repository.findById(id).ifPresent(tx -> {
            tx.setTransactionStatus(status);
            tx.setFailureReason(reason); // Add this field to your Entity
            repository.save(tx);
        });
    }
}