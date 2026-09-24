package com.ridelink.driverservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Standardised API error response for Driver Service.
 */
@Data
@Builder
@Schema(description = "Standard error response")
public class ApiError {
    private int status;
    private String message;
    private String path;
    private Instant timestamp;
}
