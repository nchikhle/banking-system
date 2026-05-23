package com.logiqpool.accountservice.service;


import com.logiqpool.accountservice.dto.AccountRequestDto;
import com.logiqpool.accountservice.dto.AccountResponseDto;
import com.logiqpool.accountservice.exception.AccountNotFoundException;
import com.logiqpool.accountservice.exception.InsufficientFundsException;
import com.logiqpool.accountservice.model.Account;
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
                .accountType(request.accountType())
                .build();

        // 3. Persist
        Account savedAccount = accountRepository.save(account);

        // 4. Return a Response DTO(Never return the Entity itself)
        return mapToResponseDto(savedAccount);

        //return "Account service logic executed";
    }

    public AccountResponseDto getAccount(String accountNumber){
        return accountRepository.findByAccountNumber(accountNumber)
                .map(this::mapToResponseDto)
                .orElseThrow(()-> new AccountNotFoundException(accountNumber));
    }

    @Transactional
    public void processBalanceChange(String accNum, BigDecimal amount, String key) {
        // 1. IDEMPOTENCY CHECK
        if (accountRepository.existsByLastProcessedTxId(key)) {
            log.info("Key {} already processed. Skipping to prevent double-charging.", key);
            return;
        }

        // 2. 🎯 FIX: Explicit Profile Existence Verification Check
        if (!accountRepository.existsByAccountNumber(accNum)) {
            throw new AccountNotFoundException(accNum);
        }
        /**
         * 🚨 Bug 2: Ambiguous Errors on Non-Existent Accounts
         * Look closely at processBalanceChange inside your AccountService:
         *
         * Java
         * rowsUpdated = accountRepository.subtractBalanceIfPossible(accNum, amount.abs());
         * // ...
         * if (rowsUpdated == 0) {
         *     throw new InsufficientFundsException("Update failed: Insufficient funds or account not found."); // ◀ HERE
         * }
         * Why it's a problem:
         * If your transaction-service calls updateBalance with a misspelled or completely non-existent account number, your custom @Modifying queries will update 0 rows.
         * Your application will immediately throw an InsufficientFundsException, returning an HTTP 400 Bad Request via your handler.
         *
         * This completely tricks your orchestration service! The transaction-service will assume the account exists but just lacked funds, when in reality, the account was completely missing. It should have returned an HTTP 404 Not Found so the orchestrator knows never to try a compensation refund on a non-existent account.
         *
         * 🛠️ The Fix:
         * Perform an explicit existence check before executing the balance changes. */

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

        // 4. PERSIST THE KEY
        accountRepository.updateLastTxId(accNum, key);
    }

    public boolean verifyTransactionStatus(String idempotencyKey) {
        log.info("Checking external processing status for key: {}", idempotencyKey);
        return accountRepository.existsByLastProcessedTxId(idempotencyKey);
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