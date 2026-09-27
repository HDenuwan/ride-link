package com.ridelink.rideservice.repository;

import com.ridelink.rideservice.model.Ride;
import com.ridelink.rideservice.model.Ride.RideStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for Ride documents.
 */
@Repository
public interface RideRepository extends MongoRepository<Ride, String> {

    List<Ride> findByPassengerId(String passengerId);

    List<Ride> findByDriverProfileId(String driverProfileId);

    List<Ride> findByStatus(RideStatus status);

    Optional<Ride> findByDriverProfileIdAndStatus(String driverProfileId, RideStatus status);
}
