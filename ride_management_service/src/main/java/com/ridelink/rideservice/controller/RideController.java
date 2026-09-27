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
}
