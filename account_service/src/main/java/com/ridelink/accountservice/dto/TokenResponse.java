package com.ridelink.accountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * Response DTO returned after a successful login, containing the JWT token.
 */
@Data
@Builder
@Schema(description = "Authentication token response")
public class TokenResponse {

    @Schema(description = "JWT Bearer token")
    private String token;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;

    @Schema(description = "Token expiry in milliseconds from now")
    private long expiresInMs;

    @Schema(description = "Account ID of the authenticated user")
    private String accountId;

    @Schema(description = "Assigned roles of the authenticated user")
    private Set<String> roles;
}
