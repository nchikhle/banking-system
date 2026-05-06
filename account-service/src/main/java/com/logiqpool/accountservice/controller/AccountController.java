package com.logiqpool.accountservice.controller;

import com.logiqpool.accountservice.dto.AccountRequestDto;
import com.logiqpool.accountservice.dto.AccountResponseDto;

import com.logiqpool.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        log.info("Creating account...");
        //return service.createAccount(account);
        return new ResponseEntity<>(accountService.createAccount(request), HttpStatus.CREATED);
    }
}