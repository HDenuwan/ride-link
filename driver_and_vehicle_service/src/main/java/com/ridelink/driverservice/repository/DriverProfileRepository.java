package com.ridelink.driverservice.repository;

import com.ridelink.driverservice.model.DriverProfile;
import com.ridelink.driverservice.model.DriverProfile.AvailabilityStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverProfileRepository extends MongoRepository<DriverProfile, String> {

    Optional<DriverProfile> findByAccountId(String accountId);

    boolean existsByAccountId(String accountId);

    boolean existsByLicenseNumber(String licenseNumber);

    List<DriverProfile> findByAvailability(AvailabilityStatus availability);

    List<DriverProfile> findByAvailabilityAndServiceArea(AvailabilityStatus availability, String serviceArea);
}
