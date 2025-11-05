# User Service Setup Guide

This guide provides instructions on how to set up and run the User Service for local development.

## 1. Prerequisites

*   **Java 17+**: Make sure you have JDK 17 or later installed.
*   **Maven**: Used for building the project and managing dependencies.
*   **PostgreSQL**: A running PostgreSQL instance is required.
*   **Docker**: (Optional) For running PostgreSQL in a container.

## 2. Database Setup

1.  **Create a PostgreSQL database** for the User Service. Let's name it `user_db`.
2.  **Create a user and grant privileges** to the `user_db` database.

    ```sql
    CREATE USER user_service_user WITH PASSWORD 'your_password';
    CREATE DATABASE user_db OWNER user_service_user;
    GRANT ALL PRIVILEGES ON DATABASE user_db TO user_service_user;
    ```

## 3. Environment Configuration

1.  Navigate to the `BE/User_service` directory.
2.  Create an `env.local` file by copying the example:
    ```bash
    cp env.local.example env.local
    ```
3.  Edit `env.local` and update the following variables:

    ```properties
    # PostgreSQL Database
    DB_URL=jdbc:postgresql://localhost:5432/user_db
    DB_USERNAME=user_service_user
    DB_PASSWORD=your_password

    # Keycloak (if integrated)
    KEYCLOAK_URL=http://localhost:8080/auth
    KEYCLOAK_REALM=my-realm
    ```

## 4. Application Configuration

The main configuration is in `src/main/resources/application.properties`. The values in `env.local` will override the properties at runtime.

## 5. Build and Run

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

1.  Open the `User_service` project in your IDE.
2.  The IDE should automatically detect it as a Maven project and download dependencies.
3.  Find the `UserServiceApplication.java` file and run its `main` method.

## 6. Verification

Once the application is running, you can verify it by accessing the health check endpoint:

```bash
curl http://localhost:8081/actuator/health
```

(Assuming the service runs on port 8081, which can be configured in `application.properties`).

You should see a response like:
```json
{
  "status": "UP"
}
```
