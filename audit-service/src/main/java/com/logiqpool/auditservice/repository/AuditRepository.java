package com.logiqpool.auditservice.repository;

import com.logiqpool.auditservice.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditRepository extends JpaRepository<AuditEvent, UUID> {

}
