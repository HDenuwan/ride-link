package com.ridelink.rideservice.service;

import com.ridelink.rideservice.client.DriverServiceClient;
import com.ridelink.rideservice.dto.RideRequest;
import com.ridelink.rideservice.dto.RideResponse;
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
        List<Map<String, Object>> availableDrivers = driverClient.getAvailableDrivers(request.getServiceArea());

        if (availableDrivers.isEmpty()) {
            throw new IllegalStateException("No drivers available in the requested area. Please try again later.");
        }

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
        driverClient.markDriverOnRide(driverProfileId);

        log.info("Ride {} created and assigned to driver {}", saved.getId(), driverProfileId);
        return toResponse(saved);
    }

    public RideResponse getById(String rideId) {
        return toResponse(findById(rideId));
    }

    public List<RideResponse> getByPassenger(String passengerId) {
        return rideRepository.findByPassengerId(passengerId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Ride findById(String id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new RideNotFoundException("Ride not found: " + id));
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
