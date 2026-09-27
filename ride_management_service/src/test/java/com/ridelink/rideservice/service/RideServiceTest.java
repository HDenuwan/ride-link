package com.ridelink.rideservice.service;

import com.ridelink.rideservice.client.DriverServiceClient;
import com.ridelink.rideservice.dto.RideRequest;
import com.ridelink.rideservice.dto.RideResponse;
import com.ridelink.rideservice.exception.RideNotFoundException;
import com.ridelink.rideservice.model.Ride;
import com.ridelink.rideservice.model.Ride.RideStatus;
import com.ridelink.rideservice.repository.RideRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RideService Unit Tests")
class RideServiceTest {

    @Mock private RideRepository rideRepository;
    @Mock private DriverServiceClient driverClient;

    @InjectMocks private RideService rideService;

    @Test
    @DisplayName("createRide: success assigns first available driver")
    void createRide_success() {
        RideRequest req = new RideRequest();
        req.setPickupLocation("Colombo Fort");
        req.setDestinationLocation("Katunayake Airport");
        req.setEstimatedDistanceKm(35.0);
        req.setServiceArea("Colombo");

        Map<String, Object> driverMap = Map.of("id", "drv-001", "accountId", "acc-002");
        when(driverClient.getAvailableDrivers("Colombo")).thenReturn(List.of(driverMap));
        when(rideRepository.save(any())).thenAnswer(inv -> {
            Ride r = inv.getArgument(0);
            r.setId("ride-001");
            return r;
        });

        RideResponse result = rideService.createRide("pass-001", req);

        assertThat(result.getId()).isEqualTo("ride-001");
        assertThat(result.getStatus()).isEqualTo("ASSIGNED");
        assertThat(result.getDriverProfileId()).isEqualTo("drv-001");
        verify(driverClient).markDriverOnRide("drv-001");
    }

    @Test
    @DisplayName("createRide: throws when no drivers available")
    void createRide_noDrivers_throws() {
        RideRequest req = new RideRequest();
        req.setPickupLocation("Colombo");
        req.setDestinationLocation("Galle");
        req.setEstimatedDistanceKm(120.0);
        req.setServiceArea("Galle");

        when(driverClient.getAvailableDrivers("Galle")).thenReturn(List.of());

        assertThatThrownBy(() -> rideService.createRide("pass-001", req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No drivers available");
    }

    @Test
    @DisplayName("getById: throws RideNotFoundException for unknown ID")
    void getById_notFound() {
        when(rideRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.getById("unknown"))
                .isInstanceOf(RideNotFoundException.class);
    }
}
