package com.ridelink.driverservice.service;

import com.ridelink.driverservice.dto.DriverProfileRequest;
import com.ridelink.driverservice.dto.DriverProfileResponse;
import com.ridelink.driverservice.exception.DriverNotFoundException;
import com.ridelink.driverservice.model.DriverProfile;
import com.ridelink.driverservice.model.DriverProfile.AvailabilityStatus;
import com.ridelink.driverservice.repository.DriverProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DriverProfileService Unit Tests")
class DriverProfileServiceTest {

    @Mock
    private DriverProfileRepository repository;

    @InjectMocks
    private DriverProfileService service;

    private DriverProfile sampleProfile;

    @BeforeEach
    void setUp() {
        sampleProfile = DriverProfile.builder()
                .id("drv-001")
                .accountId("acc-002")
                .licenseNumber("LIC-001")
                .licenseExpiry("2027-12-31")
                .availability(AvailabilityStatus.AVAILABLE)
                .serviceArea("Colombo")
                .vehicle(DriverProfile.Vehicle.builder()
                        .make("Toyota").model("Prius").year("2022")
                        .color("White").plateNumber("CAA-1234").vehicleType("SEDAN")
                        .build())
                .build();
    }

    @Test
    @DisplayName("createProfile: success with valid request")
    void createProfile_success() {
        DriverProfileRequest req = buildRequest();

        when(repository.existsByAccountId("acc-002")).thenReturn(false);
        when(repository.existsByLicenseNumber("LIC-001")).thenReturn(false);
        when(repository.save(any())).thenReturn(sampleProfile);

        DriverProfileResponse result = service.createProfile(req);

        assertThat(result.getId()).isEqualTo("drv-001");
        assertThat(result.getAvailability()).isEqualTo("AVAILABLE");
        verify(repository).save(any());
    }

    @Test
    @DisplayName("createProfile: throws if accountId already has profile")
    void createProfile_duplicateAccount_throws() {
        DriverProfileRequest req = buildRequest();
        when(repository.existsByAccountId("acc-002")).thenReturn(true);

        assertThatThrownBy(() -> service.createProfile(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("getById: returns profile for valid ID")
    void getById_success() {
        when(repository.findById("drv-001")).thenReturn(Optional.of(sampleProfile));

        DriverProfileResponse result = service.getById("drv-001");

        assertThat(result.getId()).isEqualTo("drv-001");
        assertThat(result.getServiceArea()).isEqualTo("Colombo");
    }

    @Test
    @DisplayName("getById: throws DriverNotFoundException for unknown ID")
    void getById_notFound() {
        when(repository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("unknown"))
                .isInstanceOf(DriverNotFoundException.class);
    }

    private DriverProfileRequest buildRequest() {
        DriverProfileRequest req = new DriverProfileRequest();
        req.setAccountId("acc-002");
        req.setLicenseNumber("LIC-001");
        req.setLicenseExpiry("2027-12-31");
        req.setServiceArea("Colombo");

        DriverProfileRequest.VehicleDto v = new DriverProfileRequest.VehicleDto();
        v.setMake("Toyota");
        v.setModel("Prius");
        v.setYear("2022");
        v.setColor("White");
        v.setPlateNumber("CAA-1234");
        v.setVehicleType("SEDAN");
        req.setVehicle(v);
        return req;
    }
}
