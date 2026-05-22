package com.logiqpool.transactionservice.client;

import com.logiqpool.transactionservice.config.SecurityFeignConfig;
import com.logiqpool.transactionservice.dto.AccountResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@FeignClient(name = "account-service",
        url="${account.service.url}",
        configuration = SecurityFeignConfig.class // <-- Links the token propagator here!
)
public interface AccountClient {

    @GetMapping("/api/v1/accounts/{accountNumber}")
    AccountResponseDto getAccount(@PathVariable String accountNumber);

    @PutMapping("/api/v1/accounts/{accountNumber}/update-balance")
    void updateBalance(@PathVariable String accountNumber,
                       @RequestParam BigDecimal amount,
                       @RequestHeader("X-Idempotency-Key") String key // This is the missing piece!
    );

    // 🚀 Maps directly to the new verification endpoint
    @GetMapping("/api/v1/accounts/transactions/status")
    boolean checkTransactionStatus(@RequestParam("key") String idempotencyKey);

}
