package com.ridelink.fareservice.service;

import com.ridelink.fareservice.dto.*;
import com.ridelink.fareservice.exception.PaymentNotFoundException;
import com.ridelink.fareservice.model.Payment;
import com.ridelink.fareservice.model.Payment.PaymentStatus;
import com.ridelink.fareservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Records a simulated payment for a completed ride.
     * Notifies Ride Management Service with the payment ID.
     */
    public PaymentResponse createPayment(PaymentRequest request) {
        if (paymentRepository.existsByRideId(request.getRideId())) {
            throw new IllegalStateException("Payment already exists for ride: " + request.getRideId());
        }

        FareCalculation calc = calculateFare(request.getDistanceKm());

        PaymentStatus status;
        String failureReason = null;

        if (request.isSimulateFailure()) {
            status = PaymentStatus.FAILED;
            failureReason = "Simulated payment failure: insufficient funds";
            log.warn("Payment simulation failure for ride {}", request.getRideId());
        } else {
            status = PaymentStatus.COMPLETED;
        }

        Payment payment = Payment.builder()
                .rideId(request.getRideId())
                .passengerId(request.getPassengerId())
                .driverProfileId(request.getDriverProfileId())
                .distanceKm(request.getDistanceKm())
                .baseRate(baseRate)
                .perKmRate(perKmRate)
                .distanceCharge(calc.distanceCharge)
                .surchargeAmount(calc.surchargeAmount)
                .totalFare(calc.totalFare)
                .surchargeReason(calc.surchargeReason)
                .paymentMethod(request.getPaymentMethod())
                .status(status)
                .failureReason(failureReason)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment {} created for ride {} status={}", saved.getId(), request.getRideId(), status);

        // Notify Ride Management Service about the payment ID (synchronous REST)
        notifyRideService(request.getRideId(), saved.getId());

        return toResponse(saved);
    }

    public PaymentResponse getById(String paymentId) {
        return toResponse(findById(paymentId));
    }

    public PaymentResponse getByRideId(String rideId) {
        return toResponse(paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new PaymentNotFoundException("No payment found for ride: " + rideId)));
    }

    public List<PaymentResponse> getByPassenger(String passengerId) {
        return paymentRepository.findByPassengerId(passengerId).stream()
                .map(this::toResponse).collect(Collectors.toList());
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

    private void notifyRideService(String rideId, String paymentId) {
        try {
            String url = rideServiceUrl + "/api/rides/" + rideId + "/payment";
            var body = java.util.Map.of("paymentId", paymentId);
            restTemplate.patchForObject(url, body, Void.class);
            log.info("Notified Ride Service: ride {} linked to payment {}", rideId, paymentId);
        } catch (Exception e) {
            log.error("Failed to notify Ride Service for ride {}: {}", rideId, e.getMessage());
        }
    }

    private Payment findById(String id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .rideId(p.getRideId())
                .passengerId(p.getPassengerId())
                .driverProfileId(p.getDriverProfileId())
                .distanceKm(p.getDistanceKm())
                .baseRate(p.getBaseRate())
                .distanceCharge(p.getDistanceCharge())
                .surchargeAmount(p.getSurchargeAmount())
                .surchargeReason(p.getSurchargeReason())
                .totalFare(p.getTotalFare())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getStatus().name())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    /** Internal value object for calculation results. */
    private record FareCalculation(
            BigDecimal distanceCharge,
            BigDecimal surchargeAmount,
            String surchargeReason,
            BigDecimal totalFare) {}
}
