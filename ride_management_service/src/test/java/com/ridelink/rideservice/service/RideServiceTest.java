package com.ridelink.rideservice.service;

import com.ridelink.rideservice.client.DriverServiceClient;
import com.ridelink.rideservice.dto.RideRequest;
import com.ridelink.rideservice.dto.RideResponse;
import com.ridelink.rideservice.exception.InvalidRideStateException;
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

    private Ride buildRide(RideStatus status) {
        return Ride.builder()
                .id("ride-001")
                .passengerId("pass-001")
                .driverProfileId("drv-001")
                .driverAccountId("acc-002")
                .pickupLocation("Colombo Fort")
                .destinationLocation("Katunayake Airport")
                .estimatedDistanceKm(35.0)
                .status(status)
                .build();
    }

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
    @DisplayName("acceptRide: ASSIGNED → ACCEPTED by correct driver")
    void acceptRide_success() {
        Ride ride = buildRide(RideStatus.ASSIGNED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(ride));
        when(rideRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RideResponse result = rideService.acceptRide("ride-001", "acc-002");

        assertThat(result.getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("acceptRide: throws InvalidRideStateException for wrong driver")
    void acceptRide_wrongDriver() {
        Ride ride = buildRide(RideStatus.ASSIGNED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.acceptRide("ride-001", "wrong-driver"))
                .isInstanceOf(InvalidRideStateException.class)
                .hasMessageContaining("not the assigned driver");
    }

    @Test
    @DisplayName("acceptRide: throws when ride is not in ASSIGNED state")
    void acceptRide_invalidState() {
        Ride ride = buildRide(RideStatus.IN_PROGRESS);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.acceptRide("ride-001", "acc-002"))
                .isInstanceOf(InvalidRideStateException.class)
                .hasMessageContaining("IN_PROGRESS");
    }

    @Test
    @DisplayName("startRide: ACCEPTED → IN_PROGRESS")
    void startRide_success() {
        Ride ride = buildRide(RideStatus.ACCEPTED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(ride));
        when(rideRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RideResponse result = rideService.startRide("ride-001", "acc-002");
        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("completeRide: IN_PROGRESS → COMPLETED releases driver")
    void completeRide_success() {
        Ride ride = buildRide(RideStatus.IN_PROGRESS);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(ride));
        when(rideRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RideResponse result = rideService.completeRide("ride-001", "acc-002");

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(driverClient).releaseDriver("drv-001");
    }

    @Test
    @DisplayName("cancelRide: any non-terminal state → CANCELLED")
    void cancelRide_success() {
        Ride ride = buildRide(RideStatus.ASSIGNED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(ride));
        when(rideRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RideResponse result = rideService.cancelRide("ride-001", "pass-001", "Changed plans");

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getCancellationReason()).isEqualTo("Changed plans");
        verify(driverClient).releaseDriver("drv-001");
    }

    @Test
    @DisplayName("cancelRide: throws when ride is already COMPLETED")
    void cancelRide_alreadyCompleted() {
        Ride ride = buildRide(RideStatus.COMPLETED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.cancelRide("ride-001", "pass-001", "Too late"))
                .isInstanceOf(InvalidRideStateException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    @DisplayName("getById: throws RideNotFoundException for unknown ID")
    void getById_notFound() {
        when(rideRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.getById("unknown"))
                .isInstanceOf(RideNotFoundException.class);
    }
}
