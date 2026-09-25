package com.ridelink.driverservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for updating driver's simulated current location.
 */
@Data
@Schema(description = "Location update request")
public class LocationUpdateRequest {

    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    @Schema(description = "Latitude coordinate", example = "6.9271")
    private double latitude;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    @Schema(description = "Longitude coordinate", example = "79.8612")
    private double longitude;

    @Schema(description = "Human-readable place name", example = "Colombo Fort")
    private String placeName;
}
