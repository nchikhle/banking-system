# Microservices Banking Ledger & Transactions Platform

A resilient, dual-service banking system engineered with Java, Spring Boot 3, and Spring Data JPA. The application features localized atomic database operations, custom inter-service error decoding, an automated distributed compensation layer, and a background reconciliation framework to protect data consistency.

---

## 🏛️ Implemented Architecture & Design Patterns

### 1. Atomic Concurrency Optimization
To prevent race conditions during high-volume account updates, the system utilizes database-driven atomic locks instead of heavy application-level locking loops:
* **Inline Balance Gating:** Uses localized JPQL `UPDATE` queries (`subtractBalanceIfPossible`) that bundle the business logic constraint directly into the SQL transaction layer (`WHERE a.balance >= :amount`).
* **Row-Level Sequential Processing:** Offloads mathematical mutations to PostgreSQL's internal row write locks, ensuring sequential threads cannot double-deduct or bypass balance limits.

### 2. Distributed Error Resilience & Compensation
Because the platform spans separate database boundaries (`account-service` and `transaction-service`), it implements programmatic resiliency patterns:
* **State Isolation via `REQUIRES_NEW`:** Captures transaction intent by writing a `PENDING` state to the database within an isolated propagation context before initiating external network requests.
* **Compensating Transactions (Saga Reversals):** If the initial debit phase succeeds but the subsequent credit phase fails due to a downstream system or network exception, the orchestrator triggers an explicit runtime `-REFUND` balancing adjustment.
* **Idempotency-Key Routing Engine:** Generates deterministic execution strings (e.g., `idempotencyKey + "-DEBIT"`) to ensure downstream workers can match and prevent duplicate processing loops.
* **Custom Feign Error Decoding:** Utilizes a custom Jackson-driven `ErrorDecoder` to intercept downstream REST failures, extract message payloads, and safely translate them into domain-specific exceptions.

### 3. Distributed Audit Reconciliation Framework
Features a dedicated background `TransactionCleanupScheduler` component designed to mitigate partial infrastructure execution failures (e.g., server crashing midway through a transfer):
* Periodically scans for stuck `PENDING` or unresolved ledger records.
* Outlines an automated status-verification framework to evaluate external database reality against local records.
* Outlines a self-healing engine capable of issuing asynchronous compensating transactions to reset out-of-sync accounts.

---

## 🗂️ Service Boundaries

* **Account Service:** Manages balance ledgers, processes atomic additions/subtractions, and enforces transaction processing history memory.
* **Transaction Service:** Acts as the processing orchestrator. Validates business constraints, manages transaction life cycle states, and houses the background audit reconciliation loops.

---

## ⚙️ Technical Stack

* **Language Platform:** Java (Utilizing Builder patterns, record DTO structures)
* **Core Framework:** Spring Boot 3.x
* **Data Access:** Spring Data JPA / Hibernate Natively
* **Inter-Service Mesh:** Spring Cloud OpenFeign