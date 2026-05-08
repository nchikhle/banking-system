package com.logiqpool.accountservice.service;


import com.logiqpool.accountservice.dto.AccountRequestDto;
import com.logiqpool.accountservice.dto.AccountResponseDto;
import com.logiqpool.accountservice.model.Account;
import com.logiqpool.accountservice.model.AccountStatus;
import com.logiqpool.accountservice.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccountService {

    private final AccountRepository accountrepository;

    public AccountService(AccountRepository accountrepository){
        this.accountrepository=accountrepository;
    }

    public AccountResponseDto createAccount(AccountRequestDto request) {
        log.info("Creating account for: {}", request.getAccountHolderName());

        // 1. Generate unique account number (Logic: simple random for now)
        String generatedAccountNumber ="ACC" + System.currentTimeMillis();

        // 2. Map Dto to Entity
        Account account = Account.builder()
                .accountNumber(generatedAccountNumber)
                .accountHolderName(request.getAccountHolderName())
                .balance(request.getBalance())
                .accountType(request.getAccountType())
                // .currency(request.getCurrency() !=null ? request.getCurrency():"USD")
                // .status(AccountStatus.ACTIVE)
                .build();

        // 3. Persist
        Account savedAccount = accountrepository.save(account);

        // 4. Return a Response DTO(Never return the Entity itself)
        return mapToResponseDto(savedAccount);

        //return "Account service logic executed";
    }

    private AccountResponseDto mapToResponseDto(Account account) {
        return AccountResponseDto.builder()
                .accountHolderName(account.getAccountHolderName())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .accountType(account.getAccountType())
                .build();
    }

    public Account getAccount(String accountNumber){
        return accountrepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new RuntimeException("Account not found"));
    }
}