package com.logiqpool.accountservice.dto;

import com.logiqpool.accountservice.model.AccountType;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

@Builder // Useful for creating objects in tests
//@JsonIgnoreProperties(ignoreUnknown = false)
public record AccountRequestDto(


        @NotBlank(message = "Account holder name is required")
        @Size(min = 2, max = 100, message = "Account holder name must be between 2 and 100 characters")
        String accountHolderName,

        @NotNull(message = "Initial deposit is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Balance cannot be negative")
        BigDecimal balance,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be uppercase 3-letter ISO code, e.g. GBP, USD, EUR")
        String currency,

        @NotNull(message = "Account type is required")
        AccountType accountType // "SAVINGS" or "CHECKING")

) {
}