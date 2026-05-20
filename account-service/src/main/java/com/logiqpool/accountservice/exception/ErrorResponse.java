package com.logiqpool.accountservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;


public record ErrorResponse (
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path){}