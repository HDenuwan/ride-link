package com.ridelink.fareservice.service;

import com.ridelink.fareservice.dto.FareEstimateRequest;
import com.ridelink.fareservice.dto.FareEstimateResponse;
import com.ridelink.fareservice.dto.PaymentRequest;
import com.ridelink.fareservice.dto.PaymentResponse;
import com.ridelink.fareservice.exception.PaymentNotFoundException;
import com.ridelink.fareservice.model.Payment;
import com.ridelink.fareservice.model.Payment.PaymentStatus;
import com.ridelink.fareservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FareService Unit Tests")
class FareServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private RestTemplate restTemplate;

    private FareService fareService;

    @BeforeEach
    void setUp() {
        fareService = new FareService(
                paymentRepository,
                restTemplate,
                "http://localhost:8083",
                new BigDecimal("50.00"),
                new BigDecimal("25.00"),
                20,
                15
        );
    }

    @Test
    @DisplayName("estimateFare: returns correct subtotal for daytime 10km ride")
    void estimateFare_daytime_10km() {
        FareEstimateRequest req = new FareEstimateRequest();
        req.setPickupLocation("A");
        req.setDestinationLocation("B");
        req.setDistanceKm(10.0);

        FareEstimateResponse result = fareService.estimateFare(req);

        assertThat(result.getDistanceCharge()).isEqualByComparingTo("250.00"); // 10 × 25
        assertThat(result.getBaseFare()).isEqualByComparingTo("50.00");
        // Total is 300 ± surcharge (depends on current time, so just verify it's >= 300)
        assertThat(result.getEstimatedTotal().compareTo(new BigDecimal("300.00"))).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("createPayment: success stores COMPLETED payment")
    void createPayment_success() {
        PaymentRequest req = buildPaymentRequest(false);

        when(paymentRepository.existsByRideId("ride-001")).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId("pay-001");
            return p;
        });

        PaymentResponse result = fareService.createPayment(req);

        assertThat(result.getId()).isEqualTo("pay-001");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTotalFare().compareTo(BigDecimal.ZERO)).isGreaterThan(0);
        verify(paymentRepository).save(any());
    }

    @Test
    @DisplayName("createPayment: simulateFailure=true stores FAILED payment")
    void createPayment_simulated_failure() {
        PaymentRequest req = buildPaymentRequest(true);

        when(paymentRepository.existsByRideId("ride-002")).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId("pay-002");
            return p;
        });

        PaymentResponse result = fareService.createPayment(req);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getFailureReason()).contains("Simulated payment failure");
    }

    @Test
    @DisplayName("createPayment: throws if payment already exists for ride")
    void createPayment_duplicate_throws() {
        PaymentRequest req = buildPaymentRequest(false);
        when(paymentRepository.existsByRideId("ride-001")).thenReturn(true);

        assertThatThrownBy(() -> fareService.createPayment(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("getById: throws PaymentNotFoundException for unknown ID")
    void getById_notFound() {
        when(paymentRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fareService.getById("unknown"))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    @DisplayName("getByRideId: returns correct payment for known ride")
    void getByRideId_success() {
        Payment payment = buildPayment("pay-001", "ride-001", PaymentStatus.COMPLETED);
        when(paymentRepository.findByRideId("ride-001")).thenReturn(Optional.of(payment));

        PaymentResponse result = fareService.getByRideId("ride-001");

        assertThat(result.getRideId()).isEqualTo("ride-001");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("estimateFare: formula description is included in response")
    void estimateFare_includesFormula() {
        FareEstimateRequest req = new FareEstimateRequest();
        req.setPickupLocation("X");
        req.setDestinationLocation("Y");
        req.setDistanceKm(5.0);

        FareEstimateResponse result = fareService.estimateFare(req);

        assertThat(result.getFormula()).isNotBlank();
        assertThat(result.getFormula()).contains("baseRate");
        assertThat(result.getFormula()).contains("perKmRate");
    }

    private PaymentRequest buildPaymentRequest(boolean simulateFailure) {
        PaymentRequest req = new PaymentRequest();
        req.setRideId(simulateFailure ? "ride-002" : "ride-001");
        req.setPassengerId("pass-001");
        req.setDriverProfileId("drv-001");
        req.setDistanceKm(35.0);
        req.setPaymentMethod("SIMULATED_CARD");
        req.setSimulateFailure(simulateFailure);
        return req;
    }

    private Payment buildPayment(String id, String rideId, PaymentStatus status) {
        return Payment.builder()
                .id(id)
                .rideId(rideId)
                .passengerId("pass-001")
                .driverProfileId("drv-001")
                .distanceKm(35.0)
                .baseRate(new BigDecimal("50.00"))
                .perKmRate(new BigDecimal("25.00"))
                .distanceCharge(new BigDecimal("875.00"))
                .surchargeAmount(BigDecimal.ZERO)
                .totalFare(new BigDecimal("925.00"))
                .surchargeReason("")
                .paymentMethod("SIMULATED_CARD")
                .status(status)
                .build();
    }
}
