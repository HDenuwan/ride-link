package com.ridelink.fareservice.controller;

import com.ridelink.fareservice.dto.*;
import com.ridelink.fareservice.service.FareService;
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
public class FareController {

    private final FareService fareService;

    public FareController(FareService fareService) {
        this.fareService = fareService;
    }

    // ------------------------------------------------------------------
    // Fare estimation (public endpoint)
    // ------------------------------------------------------------------

    @PostMapping("/api/fares/estimate")
    public ResponseEntity<FareEstimateResponse> estimateFare(@Valid @RequestBody FareEstimateRequest request) {
        return ResponseEntity.ok(fareService.estimateFare(request));
    }

    // ------------------------------------------------------------------
    // Payment endpoints (authenticated)
    // ------------------------------------------------------------------

    @PostMapping("/api/payments")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fareService.createPayment(request));
    }

    @GetMapping("/api/payments/{id}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(fareService.getById(id));
    }

    @GetMapping("/api/payments/ride/{rideId}")
    public ResponseEntity<PaymentResponse> getByRideId(@PathVariable String rideId) {
        return ResponseEntity.ok(fareService.getByRideId(rideId));
    }

    @GetMapping("/api/payments/my")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(Authentication auth) {
        String passengerId = (String) auth.getPrincipal();
        return ResponseEntity.ok(fareService.getByPassenger(passengerId));
    }
}
