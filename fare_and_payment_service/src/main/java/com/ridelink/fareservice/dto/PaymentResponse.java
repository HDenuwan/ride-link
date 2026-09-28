package com.ridelink.fareservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for a payment record / receipt.
 */
@Data
@Builder
@Schema(description = "Payment record / receipt response")
public class PaymentResponse {

    @Schema(description = "Payment record ID")
    private String id;

    @Schema(description = "Ride ID")
    private String rideId;

    @Schema(description = "Passenger ID")
    private String passengerId;

    @Schema(description = "Driver profile ID")
    private String driverProfileId;

    @Schema(description = "Actual ride distance in km")
    private double distanceKm;

    @Schema(description = "Base fare (LKR)")
    private BigDecimal baseRate;

    @Schema(description = "Distance charge (LKR)")
    private BigDecimal distanceCharge;

    @Schema(description = "Surcharge amount (LKR)")
    private BigDecimal surchargeAmount;

    @Schema(description = "Surcharge reason")
    private String surchargeReason;

    @Schema(description = "Total fare paid (LKR)")
    private BigDecimal totalFare;

    @Schema(description = "Payment method used")
    private String paymentMethod;

    @Schema(description = "Payment status: PENDING, COMPLETED, FAILED, REFUNDED")
    private String status;

    @Schema(description = "Failure reason (if failed)")
    private String failureReason;

    @Schema(description = "Payment creation timestamp")
    private Instant createdAt;

    @Schema(description = "Payment last update timestamp")
    private Instant updatedAt;
}
