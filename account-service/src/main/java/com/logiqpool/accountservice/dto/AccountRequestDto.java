package com.logiqpool.accountservice.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.logiqpool.accountservice.model.AccountType;
/***
 * A Security & Logic Note: The client should never send the accountNumber when creating an account. The bank's system generates that. If you let the user provide it, they could guess existing numbers or break your internal sequencing.
 *
 * Also, for a banking API, we must enforce strict validation. We don't want an account created with a balance of -5,000 or a null name.
 */

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder // Useful for creating objects in tests
//@JsonIgnoreProperties(ignoreUnknown = false)
public class AccountRequestDto {

    //@Column(unique = true, nullable = false, length = 20)

    //private String accountNumber;

    //@Column(nullable = false)
    @NotBlank(message ="Account holder name is required")
    private String accountHolderName;

    //@Column(nullable = false,precision = 19, scale = 4)
    @NotNull(message = "Initial deposite is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance cannot be negative")
    private BigDecimal balance;

   // @Column(nullable = false)
    @Size(min = 3,max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency; //e.g "USD

    @NotNull
    private AccountType accountType; // "SAVINGS" or "CHECKING"

}