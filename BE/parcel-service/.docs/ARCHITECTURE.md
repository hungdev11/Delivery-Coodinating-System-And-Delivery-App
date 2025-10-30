# Parcel Service Architecture

## 1. Overview

The Parcel Service is a Spring Boot application dedicated to managing the entire lifecycle of parcels within the delivery system. It handles parcel creation, tracking, status transitions, and history logging.

## 2. Core Technology

*   **Spring Boot**: The underlying application framework.
*   **Spring Data JPA**: For database interaction with a MySQL database.
*   **Spring Web**: For creating RESTful APIs.
*   **Spring Security**: For securing the service's endpoints.

## 3. Project Structure

```
/src/main/java/com/ds/parcel_service/
├── app_context/                # Data access layer
│   ├── models/                 # JPA entities (Parcel, ParcelLog)
│   └── repositories/           # JPA repositories
├── application/                # Web and configuration layer
│   ├── configs/                # Spring configurations
│   └── controllers/            # REST API controllers
├── business/                   # Business logic layer
│   └── v1/
│       └── services/           # Service implementations
├── common/                     # Shared components
│   ├── entities/               # DTOs and common models
│   ├── enums/                  # Enumerations (e.g., ParcelStatus)
│   ├── exceptions/             # Custom exceptions
│   ├── interfaces/             # Service interfaces
│   └── parcelstates/           # State pattern for parcel status transitions
└── ParcelServiceApplication.java # Spring Boot main application class
```

## 4. Database Schema

The service revolves around two main tables:

### `parcels`

Stores the core information for each parcel.

```sql
CREATE TABLE parcels (
  `id` VARCHAR(36) PRIMARY KEY,
  `code` VARCHAR(255) UNIQUE NOT NULL,
  `name` VARCHAR(255),
  `description` TEXT,
  `status` VARCHAR(50) NOT NULL,
  `delivery_type` VARCHAR(50),
  `sender_id` VARCHAR(36),
  `recipient_name` VARCHAR(255),
  `recipient_phone` VARCHAR(255),
  `recipient_address` VARCHAR(1000),
  `weight` DOUBLE,
  `price` DECIMAL(10, 2),
  `created_at` DATETIME,
  `updated_at` DATETIME
);
```

### `parcel_logs`

Stores the history of status changes for each parcel, providing a complete audit trail.

```sql
CREATE TABLE parcel_logs (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `parcel_id` VARCHAR(36) NOT NULL,
  `status` VARCHAR(50) NOT NULL,
  `description` VARCHAR(1000),
  `created_at` DATETIME,
  FOREIGN KEY (parcel_id) REFERENCES parcels(id)
);
```

## 5. Parcel Status Management (State Pattern)

A key feature of this service is its use of the **State design pattern** to manage parcel status transitions. This makes the status logic robust, maintainable, and easily extensible.

*   **Location**: `common/parcelstates/`
*   **`ParcelState` interface**: Defines the common methods that each state must implement (e.g., `next()`, `cancel()`).
*   **Concrete State Classes**: Each parcel status (e.g., `PendingState`, `ShippingState`, `DeliveredState`) is implemented as a separate class.

This pattern ensures that invalid status transitions are impossible (e.g., a parcel cannot go from `PENDING` directly to `DELIVERED`).
