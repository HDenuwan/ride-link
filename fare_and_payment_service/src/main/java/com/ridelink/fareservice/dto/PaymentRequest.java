package com.ridelink.fareservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for creating a payment record after ride completion.
 */
@Data
@Schema(description = "Payment creation request")
public class PaymentRequest {

    @NotBlank(message = "Ride ID is required")
    @Schema(description = "Ride Management Service ride ID", example = "ride-001")
    private String rideId;

    @NotBlank(message = "Passenger ID is required")
    @Schema(description = "Passenger account ID", example = "pass-001")
    private String passengerId;

    @NotBlank(message = "Driver profile ID is required")
    @Schema(description = "Driver profile ID", example = "drv-001")
    private String driverProfileId;

    @DecimalMin(value = "0.1")
    @Schema(description = "Actual ride distance in km", example = "34.2")
    private double distanceKm;

    @NotBlank
    @Pattern(regexp = "^(SIMULATED_CARD|SIMULATED_CASH|SIMULATED_WALLET)$",
             message = "Payment method must be SIMULATED_CARD, SIMULATED_CASH or SIMULATED_WALLET")
    @Schema(description = "Payment method",
            allowableValues = {"SIMULATED_CARD","SIMULATED_CASH","SIMULATED_WALLET"},
            example = "SIMULATED_CARD")
    private String paymentMethod;

    @Schema(description = "Simulate payment failure (for testing)", example = "false")
    private boolean simulateFailure;
}
