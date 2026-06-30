package com.logiqpool.accountservice.repository;

import com.logiqpool.accountservice.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);
}