# Session Service Architecture

## 1. Overview

The Session Service is a Spring Boot application that manages delivery sessions and tasks. It is the core component for assigning parcels to delivery personnel and tracking the lifecycle of a delivery task.

## 2. Core Technology

*   **Spring Boot**: The underlying application framework.
*   **Spring Data JPA**: For database interaction with a MySQL database.
*   **Spring Web**: For creating RESTful APIs.
*   **Spring Cloud OpenFeign**: For declarative REST API communication with other services.
*   **MapStruct**: For mapping between JPA entities and DTOs.
*   **ZXing (Zebra Crossing)**: For generating QR codes.

## 3. Project Structure

```
/src/main/java/com/ds/session/session_service/
├── app_context/                # Data access layer
│   ├── models/                 # JPA entities (Session, Task)
│   └── repositories/           # JPA repositories
├── application/                # Web and communication layer
│   ├── client/                 # OpenFeign clients for other services
│   ├── configs/                # Spring configurations
│   └── controllers/            # REST API controllers
├── business/                   # Business logic layer
│   └── v1/
│       └── services/           # Service implementations
├── common/                     # Shared components
│   ├── entities/               # DTOs and common models
│   ├── enums/                  # Enumerations (e.g., TaskStatus)
│   ├── interfaces/             # Service interfaces
│   └── mapper/                 # MapStruct mappers
└── SessionServiceApplication.java # Spring Boot main application class
```

## 4. Database Schema

The service primarily deals with `sessions` and `tasks`.

### `delivery_sessions`

Represents a collection of tasks for a single delivery run.

```sql
CREATE TABLE delivery_sessions (
  `id` VARCHAR(36) PRIMARY KEY,
  `delivery_man_id` VARCHAR(36) NOT NULL,
  `start_time` DATETIME,
  `end_time` DATETIME,
  `status` VARCHAR(50)
);
```

### `delivery_tasks`

Represents a single delivery task (e.g., delivering one parcel).

```sql
CREATE TABLE delivery_tasks (
  `id` VARCHAR(36) PRIMARY KEY,
  `session_id` VARCHAR(36),
  `parcel_id` VARCHAR(36) NOT NULL,
  `status` VARCHAR(50) NOT NULL,
  `assignment_time` DATETIME,
  `completion_time` DATETIME,
  `notes` TEXT,
  FOREIGN KEY (session_id) REFERENCES delivery_sessions(id)
);
```

## 5. Inter-Service Communication

The Session Service uses **Spring Cloud OpenFeign** to communicate with other services in a declarative way. The Feign client interfaces are defined in the `application/client` package.

This allows the Session Service to, for example, retrieve parcel details from the Parcel Service or user details from the User Service without needing to know their actual network locations.

## 6. QR Code Generation

The service uses the **ZXing (Zebra Crossing)** library to generate QR codes. This is likely used to create QR codes for delivery tasks or sessions, which can then be scanned by delivery personnel.
