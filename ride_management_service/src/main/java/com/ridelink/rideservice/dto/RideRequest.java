package com.ridelink.rideservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for creating a new ride request.
 */
@Data
@Schema(description = "Ride request creation payload")
public class RideRequest {

    @NotBlank(message = "Pickup location is required")
    @Schema(description = "Pickup location name or address", example = "Colombo Fort Railway Station")
    private String pickupLocation;

    @NotBlank(message = "Destination is required")
    @Schema(description = "Destination location name or address", example = "Bandaranaike International Airport")
    private String destinationLocation;

    @DecimalMin(value = "0.1", message = "Estimated distance must be > 0")
    @Schema(description = "Estimated distance in km (used for fare estimation)", example = "35.5")
    private double estimatedDistanceKm;

    @Schema(description = "Service area to filter available drivers", example = "Colombo")
    private String serviceArea;
}
