package com.ridelink.fareservice.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a simulated payment record for a completed ride.
 *
 * Fare Calculation Rule (documented):
 *   finalFare = baseRate + (distanceKm × perKmRate)
 *   + optional night surcharge (20%) if ride hour is between 22:00 and 06:00
 *   + optional peak surcharge (15%) if ride hour is between 07:00-09:00 or 17:00-19:00
 *   (Surcharges are additive percentages applied to the base+distance subtotal.)
 *
 * Owned exclusively by Fare & Payment Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    /** Ride Management Service ride ID (no cross-DB join) */
    @Indexed
    private String rideId;

    private String passengerId;
    private String driverProfileId;

    private double distanceKm;

    private BigDecimal baseRate;
    private BigDecimal perKmRate;
    private BigDecimal distanceCharge;
    private BigDecimal surchargeAmount;
    private BigDecimal totalFare;

    private String surchargeReason; // e.g., "Night surcharge", "Peak hours"

    /** PENDING, COMPLETED, FAILED, REFUNDED */
    private PaymentStatus status;

    /** SIMULATED_CARD, SIMULATED_CASH, SIMULATED_WALLET */
    private String paymentMethod;

    private String failureReason;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }
}
