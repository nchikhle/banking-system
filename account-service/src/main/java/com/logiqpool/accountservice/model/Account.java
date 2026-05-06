package com.logiqpool.accountservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Account {
    @Id
    @GeneratedValue
    private Long id;
    private String accountNumber;
    private String name;
    private Double balance;
}