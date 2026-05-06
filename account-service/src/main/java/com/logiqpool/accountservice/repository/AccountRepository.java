package com.logiqpool.accountservice.repository;

import com.logiqpool.accountservice.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository  extends JpaRepository<Account , UUID> {
    Optional<Account> findByAccountNumber(String accountNumber);

}
