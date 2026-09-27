package com.ridelink.rideservice.controller;

import com.ridelink.rideservice.dto.RideRequest;
import com.ridelink.rideservice.dto.RideResponse;
import com.ridelink.rideservice.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Ride Management Service.
 */
@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody RideRequest request,
                                                    Authentication auth) {
        String passengerId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(rideService.createRide(passengerId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(rideService.getById(id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<RideResponse>> getMyRides(Authentication auth) {
        String passengerId = (String) auth.getPrincipal();
        return ResponseEntity.ok(rideService.getByPassenger(passengerId));
    }

    @GetMapping("/driver/{driverProfileId}")
    public ResponseEntity<List<RideResponse>> getDriverRides(@PathVariable String driverProfileId) {
        return ResponseEntity.ok(rideService.getByDriver(driverProfileId));
    }

    @GetMapping
    public ResponseEntity<List<RideResponse>> getByStatus(@RequestParam(required = false) String status) {
        if (status == null) status = "REQUESTED";
        return ResponseEntity.ok(rideService.getByStatus(status));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<RideResponse> acceptRide(@PathVariable String id, Authentication auth) {
        String driverAccountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(rideService.acceptRide(id, driverAccountId));
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<RideResponse> startRide(@PathVariable String id, Authentication auth) {
        String driverAccountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(rideService.startRide(id, driverAccountId));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<RideResponse> completeRide(@PathVariable String id, Authentication auth) {
        String driverAccountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(rideService.completeRide(id, driverAccountId));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<RideResponse> cancelRide(@PathVariable String id,
                                                    @RequestBody(required = false) Map<String, String> body,
                                                    Authentication auth) {
        String requesterId = (String) auth.getPrincipal();
        String reason = body != null ? body.getOrDefault("reason", "Cancelled by user") : "Cancelled by user";
        return ResponseEntity.ok(rideService.cancelRide(id, requesterId, reason));
    }

    @PatchMapping("/{id}/payment")
    public ResponseEntity<Void> linkPayment(@PathVariable String id,
                                             @RequestBody Map<String, String> body) {
        rideService.linkPayment(id, body.get("paymentId"));
        return ResponseEntity.ok().build();
    }
}
