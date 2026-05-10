package com.logiqpool.accountservice.dto;

import com.logiqpool.accountservice.model.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder // Useful for creating objects in tests
//@JsonIgnoreProperties(ignoreUnknown = false)
public record AccountRequestDto(

        //@Column(unique = true, nullable = false, length = 20)

        //private String accountNumber;

        //@Column(nullable = false)
        @NotBlank(message = "Account holder name is required")
        String accountHolderName,

        //@Column(nullable = false,precision = 19, scale = 4)
        @NotNull(message = "Initial deposite is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Balance cannot be negative")
        BigDecimal balance,

        // @Column(nullable = false)
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency, //e.g "USD

        @NotNull
        AccountType accountType // "SAVINGS" or "CHECKING")

) {
}