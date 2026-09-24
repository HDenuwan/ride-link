package com.ridelink.driverservice.controller;

import com.ridelink.driverservice.dto.*;
import com.ridelink.driverservice.service.DriverProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Driver & Vehicle Service endpoints.
 */
@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverProfileService service;

    public DriverController(DriverProfileService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DriverProfileResponse> create(@Valid @RequestBody DriverProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createProfile(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverProfileResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<DriverProfileResponse> getMyProfile(Authentication auth) {
        String accountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(service.getByAccountId(accountId));
    }
}
