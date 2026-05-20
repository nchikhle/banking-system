package com.logiqpool.transactionservice.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import java.io.InputStream;
import java.util.Map;

public class CustomFeignErrorDecoder implements ErrorDecoder {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.valueOf(response.status());
        String errorMessage = "Account service error";

        try (InputStream bodyIs = response.body().asInputStream()) {
            // Read the JSON body sent by account-service's GlobalExceptionHandler
            Map<?, ?> map = objectMapper.readValue(bodyIs, Map.class);
            if (map.containsKey("message")) {
                errorMessage = map.get("message").toString();
            }
        } catch (Exception e) {
            // Fallback if parsing fails
            errorMessage = response.reason();
        }

        // Map it directly to your custom integration exception
        return new AccountServiceIntegrationException(status, errorMessage);
    }
}