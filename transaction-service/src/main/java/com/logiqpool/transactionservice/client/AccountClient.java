package com.logiqpool.transactionservice.client;

import com.logiqpool.transactionservice.dto.AccountResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "account-service", url="${account.service.url}" )
public interface AccountClient {

    @GetMapping("api/accounts/{accountNumber})")
    AccountResponseDto getAccount(@PathVariable String accountNumber);


}
