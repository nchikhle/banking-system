package com.logiqpool.transactionservice.exception;

import com.logiqpool.transactionservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class TransactionGlobalExceptionHandler {

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(
            TransactionNotFoundException ex, HttpServletRequest request) {
        log.warn("Transaction lookup failed: {}", ex.getMessage());
        return buildResponseEntity(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransaction(
            InvalidTransactionException ex, HttpServletRequest request) {
        log.warn("Invalid transaction blocked: {}", ex.getMessage());
        return buildResponseEntity(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // 🎯 FIX: Explicit validation interceptor captures payload syntax issues before they hit 500 fallback logic
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse("Request verification constraints breached");

        log.warn("Inbound transfer body payload rejected at boundary [{}]: errors: {}", request.getRequestURI(), validationErrors);
        return buildResponseEntity(HttpStatus.BAD_REQUEST, validationErrors, request);
    }

    // Handles downstream failures communicating with the Account Service
    @ExceptionHandler(AccountServiceIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleAccountServiceIntegration(
            AccountServiceIntegrationException ex, HttpServletRequest request) {

        log.error("Downstream Account Service error. Status: {}, Message: {}", ex.getStatusCode(), ex.getMessage());

        // Pass through the exact status code the account service threw (e.g., 404 or 422)
        return buildResponseEntity(ex.getStatusCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleFallback(Exception ex, HttpServletRequest request) {
        log.error("Unexpected Transaction Service failure at {}", request.getRequestURI(), ex);
        return buildResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, "Internal system failure", request);
    }

    private ResponseEntity<ErrorResponse> buildResponseEntity(
            HttpStatusCode httpStatus, String message, HttpServletRequest request) {
            int statusCodeValue = httpStatus.value();
            ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(), statusCodeValue,

                    HttpStatus.valueOf(statusCodeValue).getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(httpStatus).body(error);
    }
}
