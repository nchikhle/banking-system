package com.logiqpool.accountservice.controller;

import com.logiqpool.accountservice.dto.AccountRequestDto;
import com.logiqpool.accountservice.dto.AccountResponseDto;

import com.logiqpool.accountservice.service.AccountService;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("api/v1/accounts")
@Slf4j
public class AccountController {

    private final AccountService accountService ;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }


    @PostMapping
    public ResponseEntity<AccountResponseDto> createAccount(@Valid  @RequestBody AccountRequestDto request) {
        // The @Valid annotation triggers the rules we set in the DTO (like @DecimalMin)
        log.info("REST request to create account");
        //return service.createAccount(account);
        return new ResponseEntity<>(accountService.createAccount(request), HttpStatus.CREATED);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponseDto> getAccountDetails(@PathVariable String accountNumber){
        log.info("REST request to get account: {}", accountNumber);
        AccountResponseDto account = accountService.getAccount(accountNumber);
        log.debug("Account Details: {}", account);

        return ResponseEntity.ok(account);
    }

    /**
     * This endpoint will be called by your Transaction Service (via Feign)
     * to adjust balances during a transfer.
     */
    @PutMapping("/{accountNumber}/balance")
    public ResponseEntity<AccountResponseDto> updateBalance(@PathVariable String accountNumber,
                                                            @RequestParam BigDecimal amount){
        log.info("REST request to update balance for: {}", accountNumber);
        return ResponseEntity.ok(accountService.updateBalance(accountNumber, amount));

    }
}