package com.ridelink.fareservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for fare estimation (no ride needed).
 */
@Data
@Schema(description = "Fare estimation request")
public class FareEstimateRequest {

    @NotBlank(message = "Pickup location is required")
    @Schema(description = "Pickup location name", example = "Colombo Fort")
    private String pickupLocation;

    @NotBlank(message = "Destination is required")
    @Schema(description = "Destination name", example = "Katunayake Airport")
    private String destinationLocation;

    @DecimalMin(value = "0.1", message = "Distance must be > 0")
    @Schema(description = "Estimated distance in km", example = "35.5")
    private double distanceKm;
}
