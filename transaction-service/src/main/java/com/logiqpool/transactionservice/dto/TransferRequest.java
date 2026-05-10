package com.logiqpool.transactionservice.dto;

import com.logiqpool.transactionservice.model.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder // Useful for creating objects in tests
public class TransferRequest {


    private String fromAccountNumber ;

    private String toAccountNumber ;

    private TransactionStatus transactionStatus;

    private BigDecimal amount;

}
