package com.ridelink.driverservice.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Represents a driver's operational profile and vehicle details.
 * Owned exclusively by Driver & Vehicle Service.
 *
 * accountId links to the Account Service (loose coupling via ID only).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "driver_profiles")
public class DriverProfile {

    @Id
    private String id;

    /** Reference to Account Service account ID (no cross-DB join) */
    @Indexed(unique = true)
    private String accountId;

    private String licenseNumber;
    private String licenseExpiry; // ISO date string: YYYY-MM-DD

    /** AVAILABLE, UNAVAILABLE, ON_RIDE */
    private AvailabilityStatus availability;

    private String serviceArea; // e.g., "Colombo", "Kandy"

    private Vehicle vehicle;

    private Location currentLocation;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum AvailabilityStatus {
        AVAILABLE, UNAVAILABLE, ON_RIDE
    }

    /**
     * Embedded vehicle details document.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Vehicle {
        private String make;
        private String model;
        private String year;
        private String color;
        private String plateNumber;
        /** SEDAN, SUV, TUK_TUK, VAN */
        private String vehicleType;
    }

    /**
     * Simulated geographic coordinates.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private double latitude;
        private double longitude;
        private String placeName; // Human-readable description
        private Instant updatedAt;
    }
}
