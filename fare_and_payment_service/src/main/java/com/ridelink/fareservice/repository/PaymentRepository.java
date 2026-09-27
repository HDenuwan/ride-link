package com.ridelink.fareservice.repository;

import com.ridelink.fareservice.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> findByRideId(String rideId);

    List<Payment> findByPassengerId(String passengerId);

    List<Payment> findByDriverProfileId(String driverProfileId);

    boolean existsByRideId(String rideId);
}
