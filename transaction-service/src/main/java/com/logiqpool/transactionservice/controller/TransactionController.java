package com.logiqpool.transactionservice.controller;

import com.logiqpool.transactionservice.dto.TransferRequest;
import com.logiqpool.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {
        log.info("Processing inbound transfer request. Reference: {}", request.transactionReference());

        transactionService.processTransfer(request);
        //return ResponseEntity.accepted().body("Transfer request is processed successfully!");
        log.info("Transfer request completed successfully for Reference: {}", request.transactionReference());
        return ResponseEntity.ok().build();
    }
    // GET /{transactionId}/status
    // GET / {transactionId}
}