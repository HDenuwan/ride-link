package com.ridelink.accountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Standardised API error response body.
 */
@Data
@Builder
@Schema(description = "Standard error response")
public class ApiError {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error message", example = "Email is already registered")
    private String message;

    @Schema(description = "Request path", example = "/api/accounts/register")
    private String path;

    @Schema(description = "Timestamp of the error")
    private Instant timestamp;
}
