# User Service Architecture

## 1. Overview

The User Service is a Spring Boot application responsible for managing users, roles, and permissions. It provides RESTful APIs for all user-related operations and includes a sophisticated dynamic query system for flexible data retrieval.

## 2. Project Structure

```
/src/main/java/com/ds/user/
├── app_context/                    # Data access layer
│   └── repositories/               # JPA repositories (e.g., UserRepository)
│
├── application/                    # Web and configuration layer
│   ├── annotations/                # Custom annotations (e.g., @AuthRequired)
│   ├── configs/                    # Spring configurations (e.g., SecurityConfig, AppConfig)
│   ├── controllers/                # REST API controllers (e.g., UserController)
│   ├── security/                   # Security components (e.g., UserContext)
│   └── startup/                    # Application startup logic (e.g., data seeding)
│
├── business/                       # Business logic layer
│   └── v1/
│       └── services/               # Service implementations (e.g., UserServiceImpl)
│
├── common/                         # Shared components and utilities
│   ├── entities/                   # DTOs, request/response objects
│   ├── exceptions/                 # Custom exception classes
│   ├── helper/                     # Helper classes (e.g., Dynamic Query System)
│   ├── interfaces/                 # Service interfaces (e.g., IUserService)
│   └── utils/                      # Utility classes
│
└── UserServiceApplication.java     # Spring Boot main application class
```

## 3. Layers

### Application Layer (`application`)
*   **Controllers**: Exposes the service's functionality via REST endpoints.
*   **Security**: Handles authentication, authorization, and the user security context.
*   **Configs**: Contains all Spring Boot configurations, including security, CORS, and application beans.
*   **Startup**: Initializes data when the application starts (e.g., creating default admin users and roles).

### Business Logic Layer (`business`)
*   **Services**: Contains the core business logic. It uses repositories to interact with the database and provides implementations for the interfaces defined in the `common` layer.

### Data Access Layer (`app_context`)
*   **Repositories**: Defines JPA repositories for database operations on entities.

### Common Layer (`common`)
*   **Interfaces**: Defines the contracts (interfaces) for the services, promoting loose coupling.
*   **Entities**: Contains Data Transfer Objects (DTOs) and other common data models used across the application.
*   **Helper/Utils**: Provides reusable utilities, most notably the Dynamic Query System components.
*   **Exceptions**: Custom exceptions for handling specific error scenarios.

## 4. Key Components

*   **Dynamic Query System**: A core feature of this service. It allows clients to perform complex data filtering, sorting, and pagination via the API. See [Dynamic Query System](./DYNAMIC_QUERY.md) for more details.
*   **Authentication & Authorization**: Integrated with Keycloak (as seen in `api-gateway` docs) and uses annotations like `@AuthRequired` to protect endpoints.
*   **UserContext**: A security component that holds the information of the currently authenticated user for each request.

## 5. Data Flow

A typical request flows through the service as follows:

1.  An HTTP request hits a `@RestController` in the `application/controllers` package.
2.  A security filter or annotation in the `application/security` package intercepts the request to ensure the user is authenticated and authorized.
3.  The controller calls the appropriate method in a service from the `business/services` package.
4.  The service executes the business logic, potentially calling a repository from the `app_context/repositories` package to fetch or persist data.
5.  The repository interacts with the PostgreSQL database.
6.  The data flows back up the chain, is converted to a DTO in the `common/entities` package if necessary, and is returned as a JSON response to the client.
