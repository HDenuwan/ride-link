package com.ridelink.rideservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Response DTO for a ride.
 */
@Data
@Builder
@Schema(description = "Ride response")
public class RideResponse {

    @Schema(description = "Ride ID")
    private String id;

    @Schema(description = "Passenger account ID")
    private String passengerId;

    @Schema(description = "Assigned driver profile ID")
    private String driverProfileId;

    @Schema(description = "Assigned driver account ID")
    private String driverAccountId;

    @Schema(description = "Pickup location")
    private String pickupLocation;

    @Schema(description = "Destination location")
    private String destinationLocation;

    @Schema(description = "Estimated distance in km")
    private double estimatedDistanceKm;

    @Schema(description = "Current ride status")
    private String status;

    @Schema(description = "Cancellation reason (if cancelled)")
    private String cancellationReason;

    @Schema(description = "Payment record ID (if payment created)")
    private String paymentId;

    @Schema(description = "Ride requested timestamp")
    private Instant requestedAt;

    @Schema(description = "Driver assigned timestamp")
    private Instant assignedAt;

    @Schema(description = "Driver accepted timestamp")
    private Instant acceptedAt;

    @Schema(description = "Ride started timestamp")
    private Instant startedAt;

    @Schema(description = "Ride completed timestamp")
    private Instant completedAt;

    @Schema(description = "Ride cancelled timestamp")
    private Instant cancelledAt;
}
