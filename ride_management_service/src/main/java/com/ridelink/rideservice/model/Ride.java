package com.ridelink.rideservice.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Represents the lifecycle of a ride from request to completion.
 *
 * Valid state transitions:
 *   REQUESTED → ASSIGNED → ACCEPTED → IN_PROGRESS → COMPLETED
 *   Any non-terminal state → CANCELLED
 *
 * References to other services are by stable ID only (no cross-DB queries).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rides")
public class Ride {

    @Id
    private String id;

    /** Account Service passenger account ID */
    @Indexed
    private String passengerId;

    /** Driver & Vehicle Service driver profile ID (null until assigned) */
    @Indexed
    private String driverProfileId;

    /** Account ID of the driver (for correlation) */
    private String driverAccountId;

    private String pickupLocation;
    private String destinationLocation;
    private double estimatedDistanceKm;

    private RideStatus status;

    private String cancellationReason;

    /** Fare & Payment Service payment record ID (null until payment created) */
    private String paymentId;

    @CreatedDate
    private Instant requestedAt;

    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum RideStatus {
        REQUESTED,
        ASSIGNED,
        ACCEPTED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}
