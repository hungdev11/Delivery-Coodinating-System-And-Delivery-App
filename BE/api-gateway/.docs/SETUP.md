# API Gateway Setup Guide

This guide provides instructions on how to set up and run the API Gateway for local development.

## 1. Prerequisites

*   **Java 17+**: Make sure you have JDK 17 or later installed.
*   **Maven**: Used for building the project and managing dependencies.
*   **A running Keycloak instance**: The gateway depends on Keycloak for authentication.
*   **Running downstream services**: The gateway needs to be able to connect to the services it proxies (User, Parcel, etc.).

## 2. Environment Configuration

1.  Navigate to the `BE/api-gateway` directory.
2.  Create an `env.local` file by copying the example:
    ```bash
    cp env.local.example env.local
    ```
3.  Edit `env.local` and update the following variables:

    ```properties
    # Keycloak Configuration
    KEYCLOAK_URL=http://localhost:8080/auth
    KEYCLOAK_REALM=my-realm
    KEYCLOAK_CLIENT_ID=my-client
    KEYCLOAK_CLIENT_SECRET=my-client-secret

    # Service URLs (for routing)
    services.user.base-url=http://localhost:8081
    services.parcel.base-url=http://localhost:8082
    services.session.base-url=http://localhost:8083
    services.settings.base-url=http://localhost:8084
    services.zone.base-url=http://localhost:21503
    ```

    Make sure the service URLs point to the correct addresses where your downstream services are running.

## 3. Build and Run

### Using Maven Wrapper

1.  **Build the project**:
    ```bash
    ./mvnw clean install
    ```

2.  **Run the application**:
    ```bash
    ./mvnw spring-boot:run
    ```

### Using an IDE (e.g., IntelliJ IDEA, VS Code)

1.  Open the `api-gateway` project in your IDE.
2.  The IDE should automatically detect it as a Maven project and download dependencies.
3.  Find the `GatewayApplication.java` file and run its `main` method.

## 4. Verification

Once the application is running, you can verify it by accessing the health check endpoint:

```bash
curl http://localhost:8080/api/v1/health
```

(Assuming the gateway runs on port 8080, which is the default for Spring Boot).

You should see a response like:
```json
{
  "status": "UP",
  "service": "api-gateway",
  ...
}
```
