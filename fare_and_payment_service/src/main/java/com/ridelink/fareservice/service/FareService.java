package com.ridelink.fareservice.service;

import com.ridelink.fareservice.dto.*;
import com.ridelink.fareservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

/**
 * Fare calculation and payment simulation logic.
 *
 * ======================================================
 * DOCUMENTED FARE CALCULATION RULE
 * ======================================================
 *  totalFare = baseRate + (distanceKm × perKmRate) + surcharge
 *
 *  Where surcharge applies if:
 *    - Night hours (22:00–05:59): +20% of subtotal
 *    - Peak hours (07:00–08:59 or 17:00–18:59): +15% of subtotal
 *    - Otherwise: no surcharge
 *
 *  Example (35km, daytime):
 *    subtotal = 50 + (35 × 25) = 50 + 875 = 925 LKR
 *    surcharge = 0
 *    total = 925 LKR
 *
 *  Example (35km, night):
 *    subtotal = 925 LKR
 *    night surcharge = 925 × 0.20 = 185 LKR
 *    total = 1110 LKR
 * ======================================================
 */
@Service
public class FareService {

    private static final Logger log = LoggerFactory.getLogger(FareService.class);

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;
    private final String rideServiceUrl;

    private final BigDecimal baseRate;
    private final BigDecimal perKmRate;
    private final int nightSurchargePercent;
    private final int peakSurchargePercent;

    public FareService(PaymentRepository paymentRepository,
                       RestTemplate restTemplate,
                       @Value("${service.ride.base-url}") String rideServiceUrl,
                       @Value("${fare.base-rate}") BigDecimal baseRate,
                       @Value("${fare.per-km-rate}") BigDecimal perKmRate,
                       @Value("${fare.night-surcharge-percent}") int nightSurchargePercent,
                       @Value("${fare.peak-surcharge-percent}") int peakSurchargePercent) {
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
        this.rideServiceUrl = rideServiceUrl;
        this.baseRate = baseRate;
        this.perKmRate = perKmRate;
        this.nightSurchargePercent = nightSurchargePercent;
        this.peakSurchargePercent = peakSurchargePercent;
    }

    /**
     * Estimates fare for a proposed trip (no database write).
     */
    public FareEstimateResponse estimateFare(FareEstimateRequest request) {
        FareCalculation calc = calculateFare(request.getDistanceKm());

        return FareEstimateResponse.builder()
                .pickupLocation(request.getPickupLocation())
                .destinationLocation(request.getDestinationLocation())
                .distanceKm(request.getDistanceKm())
                .baseFare(baseRate)
                .distanceCharge(calc.distanceCharge)
                .surchargeAmount(calc.surchargeAmount)
                .surchargeReason(calc.surchargeReason)
                .estimatedTotal(calc.totalFare)
                .formula("totalFare = baseRate(" + baseRate + ") + (distanceKm × perKmRate(" + perKmRate + "))"
                        + (calc.surchargeReason.isEmpty() ? "" : " + " + calc.surchargeReason))
                .build();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Core fare calculation with time-of-day surcharges.
     */
    private FareCalculation calculateFare(double distanceKm) {
        BigDecimal dist = BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP);
        BigDecimal distanceCharge = perKmRate.multiply(dist).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = baseRate.add(distanceCharge);

        LocalTime now = LocalTime.now();
        String surchargeReason;
        int surchargePercent;

        if (isNightTime(now)) {
            surchargePercent = nightSurchargePercent;
            surchargeReason = "Night surcharge (" + nightSurchargePercent + "%)";
        } else if (isPeakTime(now)) {
            surchargePercent = peakSurchargePercent;
            surchargeReason = "Peak hours surcharge (" + peakSurchargePercent + "%)";
        } else {
            surchargePercent = 0;
            surchargeReason = "";
        }

        BigDecimal surchargeAmount = subtotal
                .multiply(BigDecimal.valueOf(surchargePercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalFare = subtotal.add(surchargeAmount);

        return new FareCalculation(distanceCharge, surchargeAmount, surchargeReason, totalFare);
    }

    private boolean isNightTime(LocalTime t) {
        return t.getHour() >= 22 || t.getHour() < 6;
    }

    private boolean isPeakTime(LocalTime t) {
        int h = t.getHour();
        return (h >= 7 && h < 9) || (h >= 17 && h < 19);
    }

    /** Internal value object for calculation results. */
    private record FareCalculation(
            BigDecimal distanceCharge,
            BigDecimal surchargeAmount,
            String surchargeReason,
            BigDecimal totalFare) {}
}
