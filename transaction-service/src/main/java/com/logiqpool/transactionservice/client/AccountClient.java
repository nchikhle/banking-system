package com.logiqpool.transactionservice.client;

import com.logiqpool.transactionservice.config.SecurityFeignConfig;
import com.logiqpool.transactionservice.dto.AccountResponseDto;
import com.logiqpool.transactionservice.dto.BalanceChangeRequestDto;
import com.logiqpool.transactionservice.dto.TransactionStatusResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "account-service",
        url = "${account.service.url}",
        configuration = SecurityFeignConfig.class
)
public interface AccountClient {

    @GetMapping("/api/v1/accounts/{accountNumber}")
    AccountResponseDto getAccount(
            @PathVariable("accountNumber") String accountNumber
    );

    @PatchMapping("/api/v1/accounts/{accountNumber}/update-balance")
    void updateBalance(
            @PathVariable("accountNumber") String accountNumber,
            @RequestHeader("X-Idempotency-Key") String key,
            @RequestBody BalanceChangeRequestDto request
    );

    @GetMapping("/api/v1/accounts/transactions/status")
    TransactionStatusResponseDto checkTransactionStatus(
            @RequestParam("key") String idempotencyKey
    );
}