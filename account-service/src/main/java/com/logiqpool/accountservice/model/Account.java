    package com.logiqpool.accountservice.model;

    import jakarta.persistence.*;
    import lombok.*;
    import java.math.BigDecimal;
    import java.util.UUID;
    import java.time.LocalDateTime;
    /***
     * The ID Type Mismatch: You used GenerationType.UUID, but declared the variable as Long. A UUID is a 128-bit identifier (String/UUID object), not a 64-bit number. This will throw a ClassCastException.
     *
     * Precision is King: In banking, BigDecimal needs explicit precision in the database to prevent the DB from defaulting to a lower scale.
     *
     * Encapsulation: While @Data is convenient, in financial entities, we often prefer @Getter and @Setter specifically to avoid overriding equals() and hashCode() in ways that break JPA collections.
     */

    @Entity
    @Table(name = "accounts")
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder // Useful for creating objects in tests
    public class Account {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id; // Never use incremental Long IDs for public-facing banking APIs (security!)

        @Column(unique = true, nullable = false, length = 20)
        private String accountNumber;

        @Column(nullable = false)
        private String accountHolderName;

        @Column(nullable = false, precision = 19, scale = 4)
        private BigDecimal balance; // NEVER use Double/Float for money. Use BigDecimal. amount?

        @Column(nullable = false)
        private String currency;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private AccountType accountType;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private AccountStatus status; // ACTIVE, FROZEN, CLOSED // Default to active for now

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private KycStatus kycStatus;

        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(nullable = false)
        private LocalDateTime updatedAt;

        /**
         * Deprecated demo-only field.
         * Real idempotency is now tracked in idempotency_records table.
         */
        @Column(length = 100)
        private String lastProcessedTxId;


        @PrePersist
        protected void onCreate() {
            LocalDateTime now = LocalDateTime.now();

            this.createdAt = now;
            this.updatedAt = now;

            if (this.balance == null) this.balance = BigDecimal.ZERO;
            if (this.status == null) this.status = AccountStatus.ACTIVE;
            if (this.kycStatus == null) this.kycStatus = KycStatus.PENDING;
            if (this.currency == null) this.currency ="USD";
        }

        @PreUpdate
        protected void onUpdate() {
            this.updatedAt = LocalDateTime.now();
        }
    }