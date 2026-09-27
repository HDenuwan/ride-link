package com.ridelink.driverservice.controller;

import com.ridelink.driverservice.dto.*;
import com.ridelink.driverservice.service.DriverProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Driver & Vehicle Service endpoints.
 */
@RestController
@RequestMapping("/api/drivers")
@Tag(name = "Driver & Vehicle Service", description = "Driver profiles, vehicles, availability, and location management")
public class DriverController {

    private final DriverProfileService service;

    public DriverController(DriverProfileService service) {
        this.service = service;
    }

    @Operation(summary = "Register driver profile", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    public ResponseEntity<DriverProfileResponse> create(@Valid @RequestBody DriverProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createProfile(request));
    }

    @Operation(summary = "Get driver profile by ID")
    @GetMapping("/{id}")
    public ResponseEntity<DriverProfileResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @Operation(summary = "Get driver profile by account ID",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DriverProfileResponse> getMyProfile(Authentication auth) {
        String accountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(service.getByAccountId(accountId));
    }

    @Operation(summary = "Get available drivers (internal / admin use)",
               description = "Returns all drivers with AVAILABLE status. Optionally filter by serviceArea.")
    @GetMapping("/available")
    public ResponseEntity<List<DriverProfileResponse>> getAvailable(
            @RequestParam(required = false) String serviceArea) {
        return ResponseEntity.ok(service.getAvailableDrivers(serviceArea));
    }

    @Operation(summary = "Update availability status",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    public ResponseEntity<DriverProfileResponse> updateAvailability(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String status = body.get("availability");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.updateAvailability(id, status));
    }

    @Operation(summary = "Update simulated location",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    public ResponseEntity<DriverProfileResponse> updateLocation(
            @PathVariable String id,
            @Valid @RequestBody LocationUpdateRequest request) {
        return ResponseEntity.ok(service.updateLocation(id, request));
    }

    @Operation(summary = "Update vehicle details",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/{id}/vehicle")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    public ResponseEntity<DriverProfileResponse> updateVehicle(
            @PathVariable String id,
            @Valid @RequestBody DriverProfileRequest.VehicleDto vehicleDto) {
        return ResponseEntity.ok(service.updateVehicle(id, vehicleDto));
    }

    @Operation(summary = "Mark driver as ON_RIDE (internal service call)",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/{id}/on-ride")
    public ResponseEntity<Void> setOnRide(@PathVariable String id) {
        service.setOnRide(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mark driver as AVAILABLE after ride (internal service call)",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/{id}/release")
    public ResponseEntity<Void> release(@PathVariable String id) {
        service.setAvailable(id);
        return ResponseEntity.ok().build();
    }
}
