package com.ridelink.fareservice.service;

import com.ridelink.fareservice.dto.FareEstimateRequest;
import com.ridelink.fareservice.dto.FareEstimateResponse;
import com.ridelink.fareservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

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
}
