package com.ridelink.fareservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@Schema(description = "Standard error response")
public class ApiError {
    private int status;
    private String message;
    private String path;
    private Instant timestamp;
}
