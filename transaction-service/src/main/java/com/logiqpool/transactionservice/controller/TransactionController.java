package com.logiqpool.transactionservice.controller;

import com.logiqpool.transactionservice.dto.TransferRequest;
import com.logiqpool.transactionservice.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@RequestBody TransferRequest request) {
        transactionService.processTransfer(request);
        //return ResponseEntity.accepted().body("Transfer request is processed successfully!");
        return ResponseEntity.ok().build();
    }
}