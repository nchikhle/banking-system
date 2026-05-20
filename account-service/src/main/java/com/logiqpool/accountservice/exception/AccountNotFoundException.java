package com.logiqpool.accountservice.exception;
/**
 * @ResponseStatus vs @RestControllerAdvice:
 * * - @ResponseStatus (Local/Quick): Quickest way to map an exception to an HTTP status code.
 * However, it relies on Spring's default error JSON structure and offers zero body customization.
 * * - @RestControllerAdvice (Global/Flexible): Centralizes all application error handling.
 * Allows you to intercept this exception and build a custom, uniform JSON error payload
 * essential for consistent microservice-to-microservice communication.
 *
 * Quick Visual Trade-off
 * With @ResponseStatus: AccountNotFoundException ➡️ Spring Default Payload (/error) ➡️ Client receives standard Spring JSON.
 *
 * With @RestControllerAdvice: AccountNotFoundException ➡️ GlobalExceptionHandler ➡️ Client receives your custom, standardized ErrorResponse JSON.
 *
 */

//@ResponseStatus(HttpStatus.NOT_FOUND)
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountNumber) {
        super(String.format("Account with AccountNumber %s not found", accountNumber));
    }
}
