package com.logiqpool.accountservice.controller;

import com.logiqpool.accountservice.dto.AccountRequestDto;
import com.logiqpool.accountservice.dto.AccountResponseDto;

import com.logiqpool.accountservice.service.AccountService;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("api/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService ;

    @PostMapping
    public ResponseEntity<AccountResponseDto> createAccount(
            @Valid  @RequestBody AccountRequestDto request) {
            // The @Valid annotation triggers the rules we set in the DTO (like @DecimalMin)

        log.info("REST request to create account");
        return new ResponseEntity<>(accountService.createAccount(request), HttpStatus.CREATED);//return service.createAccount(account);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponseDto> getAccountDetails(
            @PathVariable String accountNumber){

        log.info("REST request to get account: {}", accountNumber);
        AccountResponseDto account = accountService.getAccount(accountNumber);
        log.debug("Account Details: {}", account);
        return ResponseEntity.ok(account);
    }
    // GET /{accountNumber}/balance

    //POST /api/v1/accounts/adjust
    //{ "accountId": "123", "amount": -50.00, "transactionId": "TXN-999" }
    //Handles the direct balance update (accepts positive for deposits, negative for withdrawals).

    /*/api/v1/accounts/reverse
    { "accountId": "123", "amount": 50.00, "originalTransactionId": "TXN-999", "reason": "RECON_TIMEOUT" }
    The Self-Healer: A compensating endpoint. If a transaction timed out but the account already deducted the money, this safely adds it back.
  */


    //GET /accounts/audit-snapshot
    //POST /reconcile-balance
    //. POST /api/v1/accounts/reverse // fix balance
    //GET /audit/{transactionId}//transaction history logs
    /**
     * This endpoint will be called by your Transaction Service (via Feign)
     * to adjust balances during a transfer.
     */

    @PutMapping("/{accountNumber}/update-balance")
    public ResponseEntity<Void> updateBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestHeader("X-Idempotency-Key") String key) {

        log.info("Processing balance change: Account={}, Amount={}, Key={}", accountNumber, amount, key);
        accountService.processBalanceChange(accountNumber, amount, key);
        return ResponseEntity.ok().build();
    }

    // 🚀 The Verification Endpoint for the Scheduler
    ///api/v1/accounts/history/{transactionId}
    /// /transactions/status
    @GetMapping("/transactions/status")
    public ResponseEntity<Boolean> checkTransactionStatus(@RequestParam("key") String idempotencyKey) {
        log.info("Received external verification request for key: {}", idempotencyKey);
        boolean status = accountService.verifyTransactionStatus(idempotencyKey);
        return ResponseEntity.ok(status);
    }
}