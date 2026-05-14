package com.logiqpool.accountservice.service;


import com.logiqpool.accountservice.dto.AccountRequestDto;
import com.logiqpool.accountservice.dto.AccountResponseDto;
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
                .orElseThrow(()-> new RuntimeException("Account not found"));
    }

    @Transactional
    public AccountResponseDto updateBalance(String accountNumber, BigDecimal amount) {
        log.info("Updating balance for account {}: {}", accountNumber, amount);

        //get Account
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        //validate for balance limit
        BigDecimal newBalance = account.getBalance().add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Insufficient funds in account: " + accountNumber);
        }

        //update balance
        account.setBalance(newBalance);
        return mapToResponseDto(accountRepository.save(account));
    }

    @Transactional
    public void processBalanceChange(String accNum, BigDecimal amount, String key) {
        // 1. IDEMPOTENCY CHECK
        if (accountRepository.existsByLastProcessedTxId(key)) {
            log.info("Key {} already processed. Skipping to prevent double-charging.", key);
            return;
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
            throw new RuntimeException("Update failed: Insufficient funds or account not found.");
        }

        // 4. PERSIST THE KEY
        accountRepository.updateLastTxId(accNum, key);
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