package com.logiqpool.transactionservice.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import java.io.InputStream;
import java.util.Map;
@Slf4j
public class CustomFeignErrorDecoder implements ErrorDecoder {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.valueOf(response.status());
        String errorMessage = "Account service error";
        // 🎯 FIX: Check if body is null before reading streams to prevent NullPointerExceptions
        if (response.body() != null) {

            try (InputStream bodyIs = response.body().asInputStream()) {
            // Read the JSON body sent by account-service's GlobalExceptionHandler
            Map<?, ?> map = objectMapper.readValue(bodyIs, Map.class);
            if (map.containsKey("message")) {
                errorMessage = map.get("message").toString();
            }
            } catch (Exception e) {
                log.error("Failed to parse dynamic Feign error payload JSON context", e);
                // Fallback if parsing fails
                errorMessage = response.reason() != null ? response.reason() : "Parsing exception wrapper";
            }
        } else {
            errorMessage = response.reason() != null ? response.reason() : "No HTTP response body provided";
        }

        // Map it directly to your custom integration exception
        log.warn("Feign call failed for [{}]. Status: {}, Message: {}", methodKey, status, errorMessage);
        return new AccountServiceIntegrationException(status, errorMessage);
    }
}