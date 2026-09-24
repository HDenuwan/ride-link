package com.ridelink.driverservice.service;

import com.ridelink.driverservice.dto.*;
import com.ridelink.driverservice.exception.DriverNotFoundException;
import com.ridelink.driverservice.model.DriverProfile;
import com.ridelink.driverservice.model.DriverProfile.*;
import com.ridelink.driverservice.repository.DriverProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Business logic for Driver & Vehicle Service.
 */
@Service
public class DriverProfileService {

    private static final Logger log = LoggerFactory.getLogger(DriverProfileService.class);

    private final DriverProfileRepository repository;

    public DriverProfileService(DriverProfileRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates a new driver profile. One profile per accountId.
     */
    public DriverProfileResponse createProfile(DriverProfileRequest request) {
        if (repository.existsByAccountId(request.getAccountId())) {
            throw new IllegalStateException("Driver profile already exists for accountId: " + request.getAccountId());
        }
        if (repository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new IllegalStateException("License number already registered: " + request.getLicenseNumber());
        }

        DriverProfile profile = DriverProfile.builder()
                .accountId(request.getAccountId())
                .licenseNumber(request.getLicenseNumber())
                .licenseExpiry(request.getLicenseExpiry())
                .serviceArea(request.getServiceArea())
                .availability(AvailabilityStatus.UNAVAILABLE)
                .vehicle(mapVehicle(request.getVehicle()))
                .build();

        return toResponse(repository.save(profile));
    }

    /**
     * Retrieves driver profile by profile ID.
     */
    public DriverProfileResponse getById(String profileId) {
        return toResponse(findById(profileId));
    }

    /**
     * Retrieves driver profile by accountId.
     */
    public DriverProfileResponse getByAccountId(String accountId) {
        DriverProfile profile = repository.findByAccountId(accountId)
                .orElseThrow(() -> new DriverNotFoundException("No driver profile found for accountId: " + accountId));
        return toResponse(profile);
    }

    /**
     * Updates availability status of a driver.
     */
    public DriverProfileResponse updateAvailability(String profileId, String status) {
        AvailabilityStatus newStatus;
        try {
            newStatus = AvailabilityStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid availability status: " + status);
        }

        DriverProfile profile = findById(profileId);
        profile.setAvailability(newStatus);
        log.info("Driver {} availability set to {}", profileId, newStatus);
        return toResponse(repository.save(profile));
    }

    /**
     * Updates simulated current location of a driver.
     */
    public DriverProfileResponse updateLocation(String profileId, LocationUpdateRequest request) {
        DriverProfile profile = findById(profileId);
        profile.setCurrentLocation(Location.builder()
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .placeName(request.getPlaceName())
                .updatedAt(Instant.now())
                .build());
        return toResponse(repository.save(profile));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private DriverProfile findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException("Driver profile not found: " + id));
    }

    private Vehicle mapVehicle(DriverProfileRequest.VehicleDto dto) {
        return Vehicle.builder()
                .make(dto.getMake())
                .model(dto.getModel())
                .year(dto.getYear())
                .color(dto.getColor())
                .plateNumber(dto.getPlateNumber())
                .vehicleType(dto.getVehicleType())
                .build();
    }

    private DriverProfileResponse toResponse(DriverProfile p) {
        DriverProfileResponse.VehicleDto vehicleDto = null;
        if (p.getVehicle() != null) {
            vehicleDto = DriverProfileResponse.VehicleDto.builder()
                    .make(p.getVehicle().getMake())
                    .model(p.getVehicle().getModel())
                    .year(p.getVehicle().getYear())
                    .color(p.getVehicle().getColor())
                    .plateNumber(p.getVehicle().getPlateNumber())
                    .vehicleType(p.getVehicle().getVehicleType())
                    .build();
        }

        DriverProfileResponse.LocationDto locationDto = null;
        if (p.getCurrentLocation() != null) {
            locationDto = DriverProfileResponse.LocationDto.builder()
                    .latitude(p.getCurrentLocation().getLatitude())
                    .longitude(p.getCurrentLocation().getLongitude())
                    .placeName(p.getCurrentLocation().getPlaceName())
                    .updatedAt(p.getCurrentLocation().getUpdatedAt())
                    .build();
        }

        return DriverProfileResponse.builder()
                .id(p.getId())
                .accountId(p.getAccountId())
                .licenseNumber(p.getLicenseNumber())
                .licenseExpiry(p.getLicenseExpiry())
                .availability(p.getAvailability().name())
                .serviceArea(p.getServiceArea())
                .vehicle(vehicleDto)
                .currentLocation(locationDto)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
