package com.logiqpool.auditservice.controller;

import com.logiqpool.auditservice.dto.AuditEventRequestDto;
import com.logiqpool.auditservice.dto.AuditEventResponseDto;
import com.logiqpool.auditservice.model.AuditEvent;
import com.logiqpool.auditservice.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/logs")
public class AuditController {

    public final AuditService auditService;

    @PostMapping
    public ResponseEntity<AuditEventResponseDto> createAuditEvent(
            @Valid @RequestBody AuditEventRequestDto request) {

        log.info("Received audit event: {}", request.eventType());

        AuditEventResponseDto response =
                auditService.createAuditEvent(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
