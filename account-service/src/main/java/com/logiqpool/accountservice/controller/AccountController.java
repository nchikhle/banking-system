package com.logiqpool.accountservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    @PostMapping
    public String createAccount() {
        System.out.println("Creating account...");
        return "Account created (dummy)";
    }
}