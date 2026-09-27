package com.ridelink.accountservice.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.Set;

/**
 * Represents a user account (passenger or driver) in the RideLink platform.
 * Each service owns its own persistence; this document belongs to Account Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "accounts")
public class Account {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    @Indexed(unique = true)
    private String phone;

    private String passwordHash;

    private String firstName;
    private String lastName;

    /** Roles: PASSENGER, DRIVER, ADMIN */
    private Set<String> roles;

    /** ACTIVE, SUSPENDED, DELETED */
    private AccountStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum AccountStatus {
        ACTIVE, SUSPENDED, DELETED
    }
}
