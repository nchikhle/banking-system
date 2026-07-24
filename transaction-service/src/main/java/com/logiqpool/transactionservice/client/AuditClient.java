package com.logiqpool.transactionservice.client;

import com.logiqpool.transactionservice.dto.AuditEventRequestDto;
import com.logiqpool.transactionservice.dto.AuditEventResponseDto;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "audit-service",
        url = "${audit.service.url}"
)
public interface AuditClient {

    @PostMapping("api/v1/logs")
    public ResponseEntity<AuditEventResponseDto> createAuditEvent(
            @Valid @RequestBody AuditEventRequestDto request
    );
}