package com.ridelink.fareservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Response DTO for fare estimate.
 */
@Data
@Builder
@Schema(description = "Fare estimate response")
public class FareEstimateResponse {

    @Schema(description = "Pickup location")
    private String pickupLocation;

    @Schema(description = "Destination location")
    private String destinationLocation;

    @Schema(description = "Distance in km")
    private double distanceKm;

    @Schema(description = "Base fare (LKR)", example = "50.00")
    private BigDecimal baseFare;

    @Schema(description = "Distance charge (distanceKm × 25 LKR/km)", example = "887.50")
    private BigDecimal distanceCharge;

    @Schema(description = "Applicable surcharge amount", example = "187.50")
    private BigDecimal surchargeAmount;

    @Schema(description = "Surcharge reason (Night/Peak/None)")
    private String surchargeReason;

    @Schema(description = "Estimated total fare (LKR)", example = "1125.00")
    private BigDecimal estimatedTotal;

    @Schema(description = "Fare calculation formula used")
    private String formula;
}
