# RideLink – Backend Microservices

> IT3130 Application Development | Group Assignment

## Overview

RideLink is a backend microservices solution for a fictional ride-sharing platform built with **Java Spring Boot** and **MongoDB**.

```
┌─────────────────────────────────────────────────────────────────┐
│                        RideLink System                          │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐   │
│  │   Account    │  │   Driver &   │  │  Ride Management   │   │
│  │   Service    │  │   Vehicle    │  │     Service        │   │
│  │  Port: 8081  │  │   Service    │  │   Port: 8083       │   │
│  │              │  │  Port: 8082  │  │                    │   │
│  └──────────────┘  └──────────────┘  └────────────────────┘   │
│                                                                 │
│                    ┌──────────────────┐                        │
│                    │  Fare & Payment  │                        │
│                    │     Service      │                        │
│                    │   Port: 8084     │                        │
│                    └──────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

## Team Members

| # | Service | Primary Owner |
|---|---------|--------------|
| 1 | Account Service | Member 1 |
| 2 | Driver & Vehicle Service | Member 2 |
| 3 | Ride Management Service | Member 3 |
| 4 | Fare & Payment Service | Member 4 |

## Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.3.4
- **Database**: MongoDB (each service has its own database)
- **Authentication**: JWT (HS256)
- **API Docs**: SpringDoc OpenAPI / Swagger UI
- **Build Tool**: Maven

## Prerequisites

- Java 17+
- Maven 3.8+
- MongoDB 6.0+ running on `localhost:27017`

Start MongoDB (if installed locally):
```bash
mongod --dbpath /data/db
```

## Configuration

Each service uses environment variables with defaults. Create a `.env` or set system environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `MONGO_URI` | `mongodb://localhost:27017/ridelink_<service>` | MongoDB connection URI |
| `JWT_SECRET` | `ridelink-super-secret-key-must-be-at-least-256-bits-long-for-hs256` | JWT signing key (change in production!) |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Token validity in ms |
| `DRIVER_SERVICE_URL` | `http://localhost:8082` | Driver Service URL (used by Ride Service) |
| `FARE_SERVICE_URL` | `http://localhost:8084` | Fare Service URL |
| `RIDE_SERVICE_URL` | `http://localhost:8083` | Ride Service URL (used by Fare Service) |

> ⚠️ **Never commit real secrets. Use environment variables in production.**

## Startup Order

Start services in this order (due to inter-service REST dependencies):

```
1. Account Service     → port 8081
2. Driver Service      → port 8082
3. Ride Service        → port 8083
4. Fare Service        → port 8084
```

## Running Each Service

```powershell
# Account Service
cd account_service
.\mvnw spring-boot:run

# Driver & Vehicle Service
cd driver_and_vehicle_service
.\mvnw spring-boot:run

# Ride Management Service
cd ride_management_service
.\mvnw spring-boot:run

# Fare & Payment Service
cd fare_and_payment_service
.\mvnw spring-boot:run
```

Or with Maven:
```bash
mvn spring-boot:run
```

## Swagger UI / API Documentation

| Service | Swagger URL |
|---------|------------|
| Account Service | http://localhost:8081/swagger-ui.html |
| Driver & Vehicle Service | http://localhost:8082/swagger-ui.html |
| Ride Management Service | http://localhost:8083/swagger-ui.html |
| Fare & Payment Service | http://localhost:8084/swagger-ui.html |

## Running Tests

```powershell
# Test individual service
cd account_service
.\mvnw test

# Test all services
foreach ($svc in @("account_service","driver_and_vehicle_service","ride_management_service","fare_and_payment_service")) {
    Write-Host "Testing $svc..."
    cd e:\Denuwan\$svc
    .\mvnw test
}
```

## Sample Test Data / Credentials

### Register a passenger
```http
POST http://localhost:8081/api/accounts/register
Content-Type: application/json

{
  "firstName": "Alice",
  "lastName": "Smith",
  "email": "alice@example.com",
  "phone": "+94771234567",
  "password": "S3cr3tPass!",
  "role": "PASSENGER"
}
```

### Register a driver
```http
POST http://localhost:8081/api/accounts/register
Content-Type: application/json

{
  "firstName": "Bob",
  "lastName": "Driver",
  "email": "bob@example.com",
  "phone": "+94777654321",
  "password": "Dr1v3rPass!",
  "role": "DRIVER"
}
```

### Login
```http
POST http://localhost:8081/api/accounts/login
Content-Type: application/json

{
  "email": "alice@example.com",
  "password": "S3cr3tPass!"
}
```

### Create Driver Profile (using driver JWT)
```http
POST http://localhost:8082/api/drivers
Authorization: Bearer <driver_jwt_token>
Content-Type: application/json

{
  "accountId": "<driver_account_id>",
  "licenseNumber": "LIC-NW-123456",
  "licenseExpiry": "2027-12-31",
  "serviceArea": "Colombo",
  "vehicle": {
    "make": "Toyota",
    "model": "Prius",
    "year": "2022",
    "color": "White",
    "plateNumber": "CAA-1234",
    "vehicleType": "SEDAN"
  }
}
```

### Set Driver Available
```http
PATCH http://localhost:8082/api/drivers/<driver_profile_id>/availability
Authorization: Bearer <driver_jwt_token>
Content-Type: application/json

{
  "availability": "AVAILABLE"
}
```

### Request Fare Estimate (public)
```http
POST http://localhost:8084/api/fares/estimate
Content-Type: application/json

{
  "pickupLocation": "Colombo Fort Railway Station",
  "destinationLocation": "Bandaranaike International Airport",
  "distanceKm": 35.5
}
```

### Create Ride Request (using passenger JWT)
```http
POST http://localhost:8083/api/rides
Authorization: Bearer <passenger_jwt_token>
Content-Type: application/json

{
  "pickupLocation": "Colombo Fort Railway Station",
  "destinationLocation": "Bandaranaike International Airport",
  "estimatedDistanceKm": 35.5,
  "serviceArea": "Colombo"
}
```

## Fare Calculation Rule

```
totalFare = baseRate + (distanceKm × perKmRate) + surcharge

Default values:
  baseRate    = LKR 50.00
  perKmRate   = LKR 25.00 per km

Surcharges (applied to subtotal):
  Night hours (22:00–05:59)          → +20%
  Peak hours  (07:00–08:59, 17:00–18:59) → +15%
  Otherwise                          → no surcharge

Example (35km, daytime):
  subtotal = 50 + (35 × 25) = LKR 925.00
  surcharge = 0
  total     = LKR 925.00
```

## Interservice Communication

| Interaction | Method | Justification |
|------------|--------|---------------|
| Ride Service → Driver Service (get available drivers) | Synchronous REST | Response needed immediately for driver assignment |
| Ride Service → Driver Service (mark ON_RIDE / release) | Synchronous REST | Status change must be confirmed before proceeding |
| Fare Service → Ride Service (link payment ID) | Synchronous REST | Payment link must be recorded on ride for receipt retrieval |

## Ride Lifecycle

```
REQUESTED → ASSIGNED → ACCEPTED → IN_PROGRESS → COMPLETED
    └──────────────────────────────┘
                 CANCELLED (from any non-terminal state)
```

## Database Ownership

| Service | Database | Collection(s) |
|---------|----------|---------------|
| Account Service | `ridelink_accounts` | `accounts` |
| Driver & Vehicle Service | `ridelink_drivers` | `driver_profiles` |
| Ride Management Service | `ridelink_rides` | `rides` |
| Fare & Payment Service | `ridelink_payments` | `payments` |

> Each service queries only its own database. Cross-service data access uses IDs via REST calls.

## Version Control

This project uses **GitHub Flow**:
- `main` – integrated, demonstrable version
- `feature/*` – individual feature branches
- Pull requests required before merging to main
- CI runs on every PR and push to main
