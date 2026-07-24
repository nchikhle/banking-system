package com.logiqpool.accountservice.service;


import com.logiqpool.accountservice.client.AuditClient;
import com.logiqpool.accountservice.dto.AccountRequestDto;
import com.logiqpool.accountservice.dto.AccountResponseDto;
import com.logiqpool.accountservice.dto.AuditEventRequestDto;
import com.logiqpool.accountservice.exception.AccountNotFoundException;
import com.logiqpool.accountservice.exception.InsufficientFundsException;
import com.logiqpool.accountservice.model.Account;
import com.logiqpool.accountservice.model.AuditEventType;
import com.logiqpool.accountservice.model.AuditStatus;
import com.logiqpool.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AuditClient auditClient;

   @Transactional
    public AccountResponseDto createAccount(AccountRequestDto request) {
        log.info("Creating account for: {}", request.accountHolderName());

        // 1. Generate unique account number
        String generatedAccountNumber = "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 2. Map Dto to Entity
        Account account = Account.builder()
                .accountNumber(generatedAccountNumber)
                .accountHolderName(request.accountHolderName())
                .balance(request.balance())
                .currency(request.currency().toUpperCase())
                .accountType(request.accountType())
                .build();

        // 3. Persist
        Account savedAccount = accountRepository.save(account);

        auditClient.createAuditEvent(AuditEventRequestDto.builder()
                .correlationId("rterter") //TODO: what would be a corelation id here?
                .accountNumber(generatedAccountNumber)
                .eventType(AuditEventType.ACCOUNT_CREATED)
                .serviceName("AccountService")
                        .transactionReference("sfsdfsdfs")
                .status(AuditStatus.SUCCESS).build());

        // 4. Return a Response DTO(Never return the Entity itself)
        return mapToResponseDto(savedAccount);

        //return "Account service logic executed";
    }

    @Transactional(readOnly = true)
    public AccountResponseDto getAccount(String accountNumber){
        return accountRepository.findByAccountNumber(accountNumber)
                .map(this::mapToResponseDto)
                .orElseThrow(()-> new AccountNotFoundException(accountNumber));
    }
    //I use @Transactional(readOnly = true) for read operations because it avoids unnecessary dirty checking and clearly communicates that the method does not modify data.

    @Transactional
    public void processBalanceChange(String accNum, BigDecimal amount, String key) {
        // 1. IDEMPOTENCY CHECK
        if (accountRepository.existsByLastProcessedTxId(key)) {
            log.info("Key {} already processed. Skipping to prevent double-charging.", key);
            return;
        }

        //Explicit Profile Existence Verification Check
        if (!accountRepository.existsByAccountNumber(accNum)) {
            throw new AccountNotFoundException(accNum);
        }

        // 2. ATOMIC UPDATE
        int rowsUpdated;
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            // Withdrawal: Check balance in the same SQL query
            rowsUpdated = accountRepository.subtractBalanceIfPossible(accNum, amount.abs());
        } else {
            // Deposit: Regular update
            rowsUpdated = accountRepository.addBalance(accNum, amount);
        }

        // 3. VALIDATION
        if (rowsUpdated == 0) {
            throw new InsufficientFundsException("Transaction Rejected: Insufficient available funds for balance modification.");
        }

        // 4. Persist idempotency key for demo-level duplicate protection.
        // Production design should store all processed keys in a separate idempotency table.
        int keyRowsUpdated = accountRepository.updateLastTxId(accNum, key);
        try {
            auditClient.createAuditEvent(
                    AuditEventRequestDto.builder()
                            .correlationId(key)
                            .transactionReference(key)
                            .accountNumber(accNum)
                            .serviceName("account-service")
                            .eventType(
                                    amount.signum() < 0
                                            ? AuditEventType.DEBIT_SUCCESS
                                            : AuditEventType.CREDIT_SUCCESS
                            )
                            .status(AuditStatus.SUCCESS)
                            .remarks("Balance updated")
                            .build()
            );
        }  catch(Exception e) {
            log.error(
                    "Audit service unavailable: {}",
                    e.getMessage()
            );
        }

        if (keyRowsUpdated == 0) {
            throw new IllegalStateException(
                    "Failed to persist idempotency key for account: " + accNum
            );
        }
    }

    public boolean verifyTransactionStatus(String idempotencyKey) {
        log.info("Checking external processing status for key: {}", idempotencyKey);
        return accountRepository.existsByLastProcessedTxId(idempotencyKey);
    }

    @Transactional
    public void withdrawWithPessimisticLock(String accountNumber, BigDecimal amount) {
        // 1. Fetch account and lock the row immediately
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // 2. Business validation
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }

        // 3. Mutate data safely
        account.setBalance(account.getBalance().subtract(amount));

        // 4. Save changes (Lock is released automatically when @Transactional block ends)
        accountRepository.save(account);
    }

    private AccountResponseDto mapToResponseDto(Account account) {
        return AccountResponseDto.builder()
                .accountNumber(account.getAccountNumber()) // Critical for Transaction Service
                .accountHolderName(account.getAccountHolderName())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .accountType(account.getAccountType())
                .build();
    }


}