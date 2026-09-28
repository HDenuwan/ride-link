package com.ridelink.fareservice.controller;

import com.ridelink.fareservice.dto.*;
import com.ridelink.fareservice.service.FareService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
