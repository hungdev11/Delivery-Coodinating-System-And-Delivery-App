# API Gateway Architecture

## 1. Overview

The API Gateway is the central entry point for all client applications interacting with the delivery system's backend. It is a Spring Boot application built with Spring WebFlux, designed to handle high-throughput, non-blocking I/O.

Its primary responsibilities are:

*   **Request Routing**: Forwarding incoming requests to the appropriate downstream microservice.
*   **Authentication & Authorization**: Securing the backend by verifying credentials for every request.
*   **Cross-Cutting Concerns**: Handling SSL termination, rate limiting, logging, and CORS.

## 2. Core Technology

*   **Spring Boot**: The underlying application framework.
*   **Spring WebFlux**: Provides a reactive, non-blocking web stack, ideal for a gateway.
*   **Spring Security**: Used for authentication and authorization.
*   **Keycloak**: The identity and access management solution. The gateway acts as an OAuth2 Resource Server, validating JWTs issued by Keycloak.

## 3. Project Structure

```
/src/main/java/com/ds/gateway/
├── annotations/                # Custom annotations like @AuthRequired and @PublicRoute
├── application/                # Web and configuration layer
│   ├── configs/                # Spring configurations (e.g., routing, security)
│   ├── controllers/            # REST controllers for gateway-specific endpoints
│   └── security/               # Security components (e.g., JWT validation)
├── business/                   # Business logic for routing and filtering
│   └── v1/
├── common/                     # Shared components
│   ├── entities/               # DTOs and common models
│   └── exceptions/             # Custom exceptions
└── GatewayApplication.java     # Spring Boot main application class
```

## 4. Request Lifecycle

1.  A client sends an HTTP request to the API Gateway.
2.  The request is processed by a series of **global filters**.
3.  **Authentication Filter**: A custom filter (or Spring Security's default) intercepts the request.
    *   It extracts the JWT from the `Authorization` header.
    *   It validates the token against the Keycloak server's public key.
    *   If the token is valid, it populates the security context with the user's details and permissions.
4.  **Routing Filter**: The gateway matches the request path to a configured route.
    *   Routes are defined in the application configuration (e.g., in a `RouteLocator` bean).
    *   Example route: `/api/v1/users/**` is routed to the User Service.
5.  The request is forwarded to the appropriate downstream service (e.g., User Service, Parcel Service).
6.  The downstream service processes the request and returns a response.
7.  The API Gateway receives the response and forwards it back to the client.

## 5. Security

Security is a primary function of the gateway.

*   **Authentication**: All requests (unless marked as `@PublicRoute`) must contain a valid JWT Bearer token in the `Authorization` header.
*   **Authorization**: The gateway can perform coarse-grained authorization by checking for specific roles or scopes in the JWT before forwarding the request.
*   **Keycloak Integration**: The gateway is configured as a "resource server" in Keycloak. It fetches the public key from Keycloak to verify the signature of incoming JWTs.

See the [Keycloak Integration](./KEYCLOAK_MIGRATION.md) guide for more details.

## 6. Routing

Routing is configured via `RouteLocator` beans in the Spring configuration. This provides a programmatic way to define routes.

**Example Route Definition (Conceptual):**

```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("user_service_route", r -> r.path("/api/v1/users/**")
            .uri("lb://user-service"))
        .route("parcel_service_route", r -> r.path("/api/v1/parcels/**")
            .uri("lb://parcel-service"))
        .build();
}
```

*   `lb://user-service` indicates that the gateway uses a service discovery mechanism (like Eureka or Consul) to find the actual location of the "user-service".

