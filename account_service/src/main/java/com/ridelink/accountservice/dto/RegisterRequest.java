package com.ridelink.accountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for passenger or driver registration.
 */
@Data
@Schema(description = "Registration request payload")
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be 1-50 characters")
    @Schema(description = "First name of the user", example = "Alice")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be 1-50 characters")
    @Schema(description = "Last name of the user", example = "Smith")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "Unique email address", example = "alice@example.com")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must be 7-15 digits, optionally prefixed with +")
    @Schema(description = "Unique phone number", example = "+94771234567")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    @Schema(description = "Account password (min 8 chars)", example = "S3cr3tPass!")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(PASSENGER|DRIVER)$", message = "Role must be PASSENGER or DRIVER")
    @Schema(description = "Desired role", allowableValues = {"PASSENGER", "DRIVER"}, example = "PASSENGER")
    private String role;
}
