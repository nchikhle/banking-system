package com.logiqpool.accountservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

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
    private UUID id;

    @Column(unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false)
    private String accountHolderName;

    @Column(nullable = false,precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE; // Default to active for now

}