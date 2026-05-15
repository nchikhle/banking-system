package com.logiqpool.accountservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class AccountGlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFoundException(AccountNotFoundException exception, HttpServletRequest request){

        log.warn("Account error: {} at path: {}", exception.getMessage(), request.getRequestURI());
        return buildResponseEntity(exception, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundException(InsufficientFundsException exception, HttpServletRequest request){

        log.warn("Transaction rejected: {} at path: {}", exception.getMessage(), request.getRequestURI());
        return buildResponseEntity(exception, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            MethodArgumentNotValidException exception, HttpServletRequest request) {

        String validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse("Validation failed");

        log.warn("Validation failed at path: {} errors: {}",
                request.getRequestURI(),
                validationErrors);

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                validationErrors,
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(error);
    }

    // 2. Handle Fallback Exceptions (e.g., Catch-all 500 Server Errors)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception exception, HttpServletRequest request) {

        // CRITICAL: Log the full stack trace for 500 errors so you can debug them!
        log.error("Unhandled system exception occurred at path: {}", request.getRequestURI(), exception);
        return buildResponseEntity(exception, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private static ResponseEntity<ErrorResponse> buildResponseEntity(Exception exception, HttpStatus status, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI()
        );
        //return new ResponseEntity<>(error, httpStatus);
        return ResponseEntity.status(status).body(error);
    }
}
