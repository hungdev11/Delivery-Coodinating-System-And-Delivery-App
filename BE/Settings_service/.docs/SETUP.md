# Settings Service Setup Guide

This guide provides instructions on how to set up and run the Settings Service for local development.

## 1. Prerequisites

*   **Java 17+**: Make sure you have JDK 17 or later installed.
*   **Maven**: Used for building the project and managing dependencies.
*   **MySQL**: A running MySQL instance is required.
*   **Docker**: (Optional) For running MySQL in a container.

## 2. Database Setup

1.  **Create a MySQL database** for the Settings Service. Let's name it `settings_db`.
2.  **Create a user and grant privileges** to the `settings_db` database.

    ```sql
    CREATE DATABASE settings_db;
    CREATE USER 'settings_user'@'localhost' IDENTIFIED BY 'your_password';
    GRANT ALL PRIVILEGES ON settings_db.* TO 'settings_user'@'localhost';
    FLUSH PRIVILEGES;
    ```

## 3. Environment Configuration

1.  Navigate to the `BE/Settings_service` directory.
2.  Create an `env.local` file by copying the example if it exists, or create a new one.
3.  The primary configuration is in `src/main/resources/application.properties`. You can override these properties using environment variables or a local properties file.

    **`application.properties`:**
    ```properties
    # Database Configuration
    spring.datasource.url=jdbc:mysql://localhost:3306/settings_db
    spring.datasource.username=settings_user
    spring.datasource.password=your_password

    # Flyway (enabled by default)
    spring.flyway.enabled=true

    # Caching (using simple in-memory cache)
    spring.cache.type=simple
    ```

## 4. Database Migrations

The service uses Flyway for database migrations. The migration scripts are located in `src/main/resources/db/migration`.

Flyway will automatically run the migrations and create the necessary tables when the application starts up.

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

1.  Open the `Settings_service` project in your IDE.
2.  The IDE should automatically detect it as a Maven project and download dependencies.
3.  Find the `SettingsServiceApplication.java` file and run its `main` method.

## 6. Verification

Once the application is running, you can verify it by accessing the health check endpoint or the Swagger UI.

*   **Health Check**:
    ```bash
    curl http://localhost:8080/actuator/health
    ```
    (Assuming the service runs on port 8080).

*   **Swagger UI**:
    Open your browser and navigate to:
    `http://localhost:8080/swagger-ui.html`
