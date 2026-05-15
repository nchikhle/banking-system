package com.logiqpool.transactionservice.client;

import com.logiqpool.transactionservice.dto.AccountResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(name = "account-service", url="${account.service.url}" )
public interface AccountClient {

    @GetMapping("api/v1/accounts/{accountNumber}")
    AccountResponseDto getAccount(@PathVariable String accountNumber);

    @PutMapping("/api/v1/accounts/{accountNumber}/update-balance")
    void updateBalance(@PathVariable String accountNumber,
                       @RequestParam BigDecimal amount,
                       @RequestHeader("X-Idempotency-Key") String key // This is the missing piece!
    );

}
