package com.logiqpool.accountservice.client;


import com.logiqpool.accountservice.dto.AuditEventRequestDto;
import com.logiqpool.accountservice.dto.AuditEventResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "audit-service",
        url = "${audit.service.url}"
)
public interface AuditClient {

    @PostMapping("api/v1/logs")
    public ResponseEntity<AuditEventResponseDto> createAuditEvent(
            @RequestBody AuditEventRequestDto auditEventRequest
    );
}