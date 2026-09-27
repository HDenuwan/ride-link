package com.ridelink.accountservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables MongoDB auditing so @CreatedDate and @LastModifiedDate are auto-populated.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
