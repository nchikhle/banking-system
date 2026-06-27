package com.logiqpool.accountservice.controller;

import com.logiqpool.accountservice.dto.AccountRequestDto;
import com.logiqpool.accountservice.dto.AccountResponseDto;

import com.logiqpool.accountservice.dto.BalanceChangeRequestDto;
import com.logiqpool.accountservice.dto.TransactionStatusResponseDto;
import com.logiqpool.accountservice.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/accounts")
@Slf4j
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService ;

    @PostMapping
    public ResponseEntity<AccountResponseDto> createAccount(
            @Valid  @RequestBody AccountRequestDto request) {
            // The @Valid annotation triggers the rules we set in the DTO (like @DecimalMin)

        log.info("REST request to create account for holder: {}", request.accountHolderName());

        AccountResponseDto response = accountService.createAccount(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponseDto> getAccountDetails(
            @PathVariable
            @NotBlank(message = "Account number is required")
            String accountNumber) {

        log.info("REST request to get account: {}", accountNumber);

        AccountResponseDto account = accountService.getAccount(accountNumber);

        log.debug("Account Details: {}", account);

        return ResponseEntity.ok(account);
    }

    // GET /{accountNumber}/balance
    //GET /accounts/audit-snapshot
    //POST /reconcile-balance
    //. POST /api/v1/accounts/reverse // fix balance
    //GET /audit/{transactionId}//transaction history logs


    /**
     * This endpoint will be called by your Transaction Service (via Feign)
     * to adjust balances during a transfer.
     *
     * Positive amount = deposit
     * Negative amount = withdrawal
     */
    @PatchMapping("/{accountNumber}/update-balance")
    public ResponseEntity<Void> updateBalance(
            @PathVariable
            @NotBlank(message = "Account number is required")
            String accountNumber,

            @RequestHeader("X-Idempotency-Key")
            @NotBlank(message = "Idempotency key is required")
            String key,

            @Valid @RequestBody BalanceChangeRequestDto request) {

        log.info("Processing balance change: Account={}, Amount={}, Key={}",
                accountNumber, request.amount(), key);

        accountService.processBalanceChange(accountNumber, request.amount(), key);

        return ResponseEntity.noContent().build();
    }

    // 🚀 The Verification Endpoint for the Scheduler
    ///api/v1/accounts/history/{transactionId}
    /// /transactions/status
    @GetMapping("/transactions/status")
    public ResponseEntity<TransactionStatusResponseDto> checkTransactionStatus(
            @RequestParam("key")
            @NotBlank(message = "Idempotency key is required")
            String idempotencyKey) {

        log.info("Received external verification request for key: {}", idempotencyKey);

        boolean processed = accountService.verifyTransactionStatus(idempotencyKey);

        return ResponseEntity.ok(
                TransactionStatusResponseDto.builder()
                        .idempotencyKey(idempotencyKey)
                        .processed(processed)
                        .build()
        );
    }
}