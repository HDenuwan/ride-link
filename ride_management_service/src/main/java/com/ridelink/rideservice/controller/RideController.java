package com.ridelink.rideservice.controller;

import com.ridelink.rideservice.dto.RideRequest;
import com.ridelink.rideservice.dto.RideResponse;
import com.ridelink.rideservice.service.RideService;
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
 * REST controller for Ride Management Service.
 */
@RestController
@RequestMapping("/api/rides")
@Tag(name = "Ride Management Service", description = "Ride request, assignment, lifecycle and retrieval")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @Operation(summary = "Create a ride request (passenger)",
               description = "Creates a ride request and auto-assigns the first available driver.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody RideRequest request,
                                                    Authentication auth) {
        String passengerId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(rideService.createRide(passengerId, request));
    }

    @Operation(summary = "Get ride by ID",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(rideService.getById(id));
    }

    @Operation(summary = "Get my rides (passenger view)",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/my")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<List<RideResponse>> getMyRides(Authentication auth) {
        String passengerId = (String) auth.getPrincipal();
        return ResponseEntity.ok(rideService.getByPassenger(passengerId));
    }

    @Operation(summary = "Get rides assigned to my driver profile",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/driver/{driverProfileId}")
    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    public ResponseEntity<List<RideResponse>> getDriverRides(@PathVariable String driverProfileId) {
        return ResponseEntity.ok(rideService.getByDriver(driverProfileId));
    }

    @Operation(summary = "Get rides by status (admin)",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RideResponse>> getByStatus(@RequestParam(required = false) String status) {
        if (status == null) status = "REQUESTED";
        return ResponseEntity.ok(rideService.getByStatus(status));
    }

    @Operation(summary = "Accept an assigned ride (driver)",
               description = "Transition: ASSIGNED → ACCEPTED",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> acceptRide(@PathVariable String id, Authentication auth) {
        String driverAccountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(rideService.acceptRide(id, driverAccountId));
    }

    @Operation(summary = "Start the ride (driver)",
               description = "Transition: ACCEPTED → IN_PROGRESS",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> startRide(@PathVariable String id, Authentication auth) {
        String driverAccountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(rideService.startRide(id, driverAccountId));
    }

    @Operation(summary = "Complete the ride (driver)",
               description = "Transition: IN_PROGRESS → COMPLETED",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<RideResponse> completeRide(@PathVariable String id, Authentication auth) {
        String driverAccountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(rideService.completeRide(id, driverAccountId));
    }

    @Operation(summary = "Cancel a ride (passenger or admin)",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<RideResponse> cancelRide(@PathVariable String id,
                                                    @RequestBody(required = false) Map<String, String> body,
                                                    Authentication auth) {
        String requesterId = (String) auth.getPrincipal();
        String reason = body != null ? body.getOrDefault("reason", "Cancelled by user") : "Cancelled by user";
        return ResponseEntity.ok(rideService.cancelRide(id, requesterId, reason));
    }

    @Operation(summary = "Link payment record to ride (internal/admin)",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/{id}/payment")
    public ResponseEntity<Void> linkPayment(@PathVariable String id,
                                             @RequestBody Map<String, String> body) {
        rideService.linkPayment(id, body.get("paymentId"));
        return ResponseEntity.ok().build();
    }
}
