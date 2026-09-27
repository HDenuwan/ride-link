package com.ridelink.rideservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configures the RestTemplate used for synchronous inter-service REST calls.
 * Design decision: synchronous REST chosen for ride/driver interactions
 * because the response (available drivers list) is needed immediately to
 * complete the ride-request workflow. An async queue would require
 * a callback mechanism, adding unnecessary complexity for this use case.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
