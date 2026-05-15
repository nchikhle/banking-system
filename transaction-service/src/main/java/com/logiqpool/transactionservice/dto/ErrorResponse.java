package com.logiqpool.transactionservice.dto;

import java.time.LocalDateTime;

/**
 * A standardized data carrier for consistent REST API error payloads 
 * across the entire banking microservice ecosystem.
 */
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path
) {}