package com.ridelink.rideservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * REST client for communicating with the Driver & Vehicle Service.
 *
 * Communication approach: synchronous REST (HTTP).
 * Justification: driver availability queries must return results immediately
 * for ride assignment; a passenger cannot proceed without knowing which
 * drivers are available. Asynchronous messaging (e.g., RabbitMQ) would
 * require a polling/callback cycle, increasing latency and complexity
 * for this read-heavy, time-sensitive operation.
 */
@Component
public class DriverServiceClient {

    private static final Logger log = LoggerFactory.getLogger(DriverServiceClient.class);

    private final RestTemplate restTemplate;
    private final String driverServiceUrl;

    public DriverServiceClient(RestTemplate restTemplate,
                               @Value("${service.driver.base-url}") String driverServiceUrl) {
        this.restTemplate = restTemplate;
        this.driverServiceUrl = driverServiceUrl;
    }

    /**
     * Fetches available drivers from the Driver Service, optionally filtered by serviceArea.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAvailableDrivers(String serviceArea) {
        String url = driverServiceUrl + "/api/drivers/available";
        if (serviceArea != null && !serviceArea.isBlank()) {
            url += "?serviceArea=" + serviceArea;
        }
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.error("Failed to fetch available drivers from Driver Service: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Marks a driver as ON_RIDE after assignment.
     */
    public void markDriverOnRide(String driverProfileId) {
        String url = driverServiceUrl + "/api/drivers/" + driverProfileId + "/on-ride";
        try {
            restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity.EMPTY, Void.class);
            log.info("Marked driver {} as ON_RIDE", driverProfileId);
        } catch (Exception e) {
            log.error("Failed to mark driver {} as ON_RIDE: {}", driverProfileId, e.getMessage());
        }
    }

    /**
     * Releases a driver back to AVAILABLE after ride completion or cancellation.
     */
    public void releaseDriver(String driverProfileId) {
        String url = driverServiceUrl + "/api/drivers/" + driverProfileId + "/release";
        try {
            restTemplate.exchange(url, HttpMethod.PATCH, HttpEntity.EMPTY, Void.class);
            log.info("Released driver {} to AVAILABLE", driverProfileId);
        } catch (Exception e) {
            log.error("Failed to release driver {}: {}", driverProfileId, e.getMessage());
        }
    }
}
