package com.ridelink.accountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for updating profile fields. All fields are optional (partial update).
 */
@Data
@Schema(description = "Profile update request (all fields optional)")
public class UpdateProfileRequest {

    @Size(min = 1, max = 50)
    @Schema(description = "Updated first name", example = "Alicia")
    private String firstName;

    @Size(min = 1, max = 50)
    @Schema(description = "Updated last name", example = "Jones")
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone must be 7-15 digits")
    @Schema(description = "Updated phone number", example = "+94779876543")
    private String phone;
}
