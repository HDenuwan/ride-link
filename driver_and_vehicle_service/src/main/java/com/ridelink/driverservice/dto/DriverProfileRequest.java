package com.ridelink.driverservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for creating or updating a driver operational profile.
 */
@Data
@Schema(description = "Driver profile create/update request")
public class DriverProfileRequest {

    @NotBlank(message = "Account ID is required")
    @Schema(description = "Account Service account ID of this driver", example = "acc-001")
    private String accountId;

    @NotBlank(message = "License number is required")
    @Schema(description = "Driver's license number", example = "LIC-NW-123456")
    private String licenseNumber;

    @NotBlank(message = "License expiry is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "License expiry must be YYYY-MM-DD")
    @Schema(description = "License expiry date (YYYY-MM-DD)", example = "2027-12-31")
    private String licenseExpiry;

    @NotBlank(message = "Service area is required")
    @Schema(description = "Primary service area (city/district)", example = "Colombo")
    private String serviceArea;

    @Valid
    @NotNull(message = "Vehicle details are required")
    @Schema(description = "Vehicle information")
    private VehicleDto vehicle;

    @Data
    @Schema(description = "Vehicle details")
    public static class VehicleDto {

        @NotBlank
        @Schema(description = "Vehicle make", example = "Toyota")
        private String make;

        @NotBlank
        @Schema(description = "Vehicle model", example = "Prius")
        private String model;

        @NotBlank
        @Pattern(regexp = "\\d{4}", message = "Year must be a 4-digit year")
        @Schema(description = "Manufacturing year", example = "2022")
        private String year;

        @NotBlank
        @Schema(description = "Vehicle color", example = "White")
        private String color;

        @NotBlank
        @Schema(description = "License plate number", example = "CAA-1234")
        private String plateNumber;

        @NotBlank
        @Pattern(regexp = "^(SEDAN|SUV|TUK_TUK|VAN)$", message = "Vehicle type must be SEDAN, SUV, TUK_TUK or VAN")
        @Schema(description = "Vehicle type", allowableValues = {"SEDAN","SUV","TUK_TUK","VAN"}, example = "SEDAN")
        private String vehicleType;
    }
}
