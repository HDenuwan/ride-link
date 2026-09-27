package com.ridelink.accountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

/**
 * Response DTO for account information (no sensitive data exposed).
 */
@Data
@Builder
@Schema(description = "Account profile response")
public class AccountResponse {

    @Schema(description = "Unique account identifier")
    private String id;

    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Last name")
    private String lastName;

    @Schema(description = "Email address")
    private String email;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Assigned roles")
    private Set<String> roles;

    @Schema(description = "Account status")
    private String status;

    @Schema(description = "Account creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}
