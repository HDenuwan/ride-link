package com.ridelink.fareservice.controller;

import com.ridelink.fareservice.dto.*;
import com.ridelink.fareservice.service.FareService;
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

/**
 * REST controller for Fare & Payment Service.
 */
@RestController
@Tag(name = "Fare & Payment Service", description = "Fare estimation, payment recording and receipt retrieval")
public class FareController {

    private final FareService fareService;

    public FareController(FareService fareService) {
        this.fareService = fareService;
    }

    // ------------------------------------------------------------------
    // Fare estimation (public endpoint)
    // ------------------------------------------------------------------

    @Operation(summary = "Estimate fare for a trip (public)",
               description = "Returns a detailed fare breakdown. No login required.")
    @PostMapping("/api/fares/estimate")
    public ResponseEntity<FareEstimateResponse> estimateFare(@Valid @RequestBody FareEstimateRequest request) {
        return ResponseEntity.ok(fareService.estimateFare(request));
    }

    // ------------------------------------------------------------------
    // Payment endpoints (authenticated)
    // ------------------------------------------------------------------

    @Operation(summary = "Create a payment for a completed ride",
               description = "Records simulated payment. Use simulateFailure=true to test failure scenario.",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/api/payments")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fareService.createPayment(request));
    }

    @Operation(summary = "Get payment by ID",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/api/payments/{id}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(fareService.getById(id));
    }

    @Operation(summary = "Get payment receipt by ride ID",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/api/payments/ride/{rideId}")
    public ResponseEntity<PaymentResponse> getByRideId(@PathVariable String rideId) {
        return ResponseEntity.ok(fareService.getByRideId(rideId));
    }

    @Operation(summary = "Get my payments (passenger)",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/api/payments/my")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(Authentication auth) {
        String passengerId = (String) auth.getPrincipal();
        return ResponseEntity.ok(fareService.getByPassenger(passengerId));
    }
}
