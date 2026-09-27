package com.ridelink.rideservice.service;

import com.ridelink.rideservice.client.DriverServiceClient;
import com.ridelink.rideservice.dto.RideRequest;
import com.ridelink.rideservice.dto.RideResponse;
import com.ridelink.rideservice.exception.InvalidRideStateException;
import com.ridelink.rideservice.exception.RideNotFoundException;
import com.ridelink.rideservice.model.Ride;
import com.ridelink.rideservice.model.Ride.RideStatus;
import com.ridelink.rideservice.repository.RideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic for Ride Management Service.
 *
 * Driver assignment strategy: First-available selection.
 * From the list of AVAILABLE drivers returned by Driver Service (sorted
 * by the Driver Service), the first driver is selected. This is a simple,
 * documented strategy; it can be replaced with scoring/distance-based
 * selection without changing the API contract.
 */
@Service
public class RideService {

    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    private final RideRepository rideRepository;
    private final DriverServiceClient driverClient;

    public RideService(RideRepository rideRepository, DriverServiceClient driverClient) {
        this.rideRepository = rideRepository;
        this.driverClient = driverClient;
    }

    /**
     * Creates a ride request and automatically assigns the first available driver
     * in the requested service area.
     */
    public RideResponse createRide(String passengerId, RideRequest request) {
        // Query available drivers from Driver Service (synchronous REST)
        List<Map<String, Object>> availableDrivers = driverClient.getAvailableDrivers(request.getServiceArea());

        if (availableDrivers.isEmpty()) {
            throw new IllegalStateException("No drivers available in the requested area. Please try again later.");
        }

        // First-available assignment strategy (documented simple approach)
        Map<String, Object> selectedDriver = availableDrivers.get(0);
        String driverProfileId = (String) selectedDriver.get("id");
        String driverAccountId = (String) selectedDriver.get("accountId");

        Ride ride = Ride.builder()
                .passengerId(passengerId)
                .driverProfileId(driverProfileId)
                .driverAccountId(driverAccountId)
                .pickupLocation(request.getPickupLocation())
                .destinationLocation(request.getDestinationLocation())
                .estimatedDistanceKm(request.getEstimatedDistanceKm())
                .status(RideStatus.ASSIGNED)
                .assignedAt(Instant.now())
                .build();

        Ride saved = rideRepository.save(ride);

        // Notify Driver Service to mark driver as ON_RIDE
        driverClient.markDriverOnRide(driverProfileId);

        log.info("Ride {} created and assigned to driver {}", saved.getId(), driverProfileId);
        return toResponse(saved);
    }

    /**
     * Driver accepts the assigned ride. Transition: ASSIGNED → ACCEPTED.
     */
    public RideResponse acceptRide(String rideId, String driverAccountId) {
        Ride ride = findById(rideId);
        validateTransition(ride.getStatus(), RideStatus.ASSIGNED, RideStatus.ACCEPTED);

        if (!driverAccountId.equals(ride.getDriverAccountId())) {
            throw new InvalidRideStateException("You are not the assigned driver for this ride.");
        }

        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(Instant.now());
        return toResponse(rideRepository.save(ride));
    }

    /**
     * Driver starts the ride. Transition: ACCEPTED → IN_PROGRESS.
     */
    public RideResponse startRide(String rideId, String driverAccountId) {
        Ride ride = findById(rideId);
        validateTransition(ride.getStatus(), RideStatus.ACCEPTED, RideStatus.IN_PROGRESS);

        if (!driverAccountId.equals(ride.getDriverAccountId())) {
            throw new InvalidRideStateException("You are not the assigned driver for this ride.");
        }

        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(Instant.now());
        return toResponse(rideRepository.save(ride));
    }

    /**
     * Driver completes the ride. Transition: IN_PROGRESS → COMPLETED.
     * Also releases the driver back to AVAILABLE.
     */
    public RideResponse completeRide(String rideId, String driverAccountId) {
        Ride ride = findById(rideId);
        validateTransition(ride.getStatus(), RideStatus.IN_PROGRESS, RideStatus.COMPLETED);

        if (!driverAccountId.equals(ride.getDriverAccountId())) {
            throw new InvalidRideStateException("You are not the assigned driver for this ride.");
        }

        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(Instant.now());
        Ride saved = rideRepository.save(ride);

        // Notify Driver Service to release driver
        driverClient.releaseDriver(ride.getDriverProfileId());

        log.info("Ride {} completed by driver {}", rideId, driverAccountId);
        return toResponse(saved);
    }

    /**
     * Cancels a ride from any non-terminal state.
     * Releases the driver if one was assigned.
     */
    public RideResponse cancelRide(String rideId, String requesterId, String reason) {
        Ride ride = findById(rideId);

        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new InvalidRideStateException(
                    "Cannot cancel a ride in " + ride.getStatus() + " state.");
        }

        boolean wasAssigned = ride.getDriverProfileId() != null
                && ride.getStatus() != RideStatus.REQUESTED;

        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancellationReason(reason);
        ride.setCancelledAt(Instant.now());
        Ride saved = rideRepository.save(ride);

        if (wasAssigned) {
            driverClient.releaseDriver(ride.getDriverProfileId());
        }

        log.info("Ride {} cancelled by {} reason: {}", rideId, requesterId, reason);
        return toResponse(saved);
    }

    /**
     * Updates the paymentId on a ride (called after Fare Service creates a payment).
     */
    public void linkPayment(String rideId, String paymentId) {
        Ride ride = findById(rideId);
        ride.setPaymentId(paymentId);
        rideRepository.save(ride);
    }

    // ---- Retrieval ----

    public RideResponse getById(String rideId) {
        return toResponse(findById(rideId));
    }

    public List<RideResponse> getByPassenger(String passengerId) {
        return rideRepository.findByPassengerId(passengerId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<RideResponse> getByDriver(String driverProfileId) {
        return rideRepository.findByDriverProfileId(driverProfileId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<RideResponse> getByStatus(String status) {
        try {
            RideStatus rs = RideStatus.valueOf(status.toUpperCase());
            return rideRepository.findByStatus(rs).stream()
                    .map(this::toResponse).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid ride status: " + status);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Ride findById(String id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new RideNotFoundException("Ride not found: " + id));
    }

    /**
     * Enforces valid state transitions.
     * Only the declared CURRENT → NEXT transition is permitted.
     */
    private void validateTransition(RideStatus current, RideStatus requiredCurrent, RideStatus target) {
        if (current != requiredCurrent) {
            throw new InvalidRideStateException(
                    "Cannot transition to " + target + ": ride is currently in " + current + " state. " +
                    "Expected: " + requiredCurrent);
        }
    }

    private RideResponse toResponse(Ride r) {
        return RideResponse.builder()
                .id(r.getId())
                .passengerId(r.getPassengerId())
                .driverProfileId(r.getDriverProfileId())
                .driverAccountId(r.getDriverAccountId())
                .pickupLocation(r.getPickupLocation())
                .destinationLocation(r.getDestinationLocation())
                .estimatedDistanceKm(r.getEstimatedDistanceKm())
                .status(r.getStatus().name())
                .cancellationReason(r.getCancellationReason())
                .paymentId(r.getPaymentId())
                .requestedAt(r.getRequestedAt())
                .assignedAt(r.getAssignedAt())
                .acceptedAt(r.getAcceptedAt())
                .startedAt(r.getStartedAt())
                .completedAt(r.getCompletedAt())
                .cancelledAt(r.getCancelledAt())
                .build();
    }
}
