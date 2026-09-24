package com.ridelink.driverservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Response DTO for a driver profile.
 */
@Data
@Builder
@Schema(description = "Driver profile response")
public class DriverProfileResponse {

    @Schema(description = "Driver profile ID")
    private String id;

    @Schema(description = "Linked account ID")
    private String accountId;

    @Schema(description = "License number")
    private String licenseNumber;

    @Schema(description = "License expiry date")
    private String licenseExpiry;

    @Schema(description = "Availability status: AVAILABLE, UNAVAILABLE, ON_RIDE")
    private String availability;

    @Schema(description = "Primary service area")
    private String serviceArea;

    @Schema(description = "Vehicle details")
    private VehicleDto vehicle;

    @Schema(description = "Simulated current location")
    private LocationDto currentLocation;

    @Schema(description = "Profile creation timestamp")
    private Instant createdAt;

    @Schema(description = "Profile last update timestamp")
    private Instant updatedAt;

    @Data
    @Builder
    public static class VehicleDto {
        private String make;
        private String model;
        private String year;
        private String color;
        private String plateNumber;
        private String vehicleType;
    }

    @Data
    @Builder
    public static class LocationDto {
        private double latitude;
        private double longitude;
        private String placeName;
        private Instant updatedAt;
    }
}
