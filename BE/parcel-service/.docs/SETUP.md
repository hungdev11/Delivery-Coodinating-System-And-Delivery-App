# Parcel Service Setup Guide

This guide provides instructions on how to set up and run the Parcel Service for local development.

## 1. Prerequisites

*   **Java 17+**: Make sure you have JDK 17 or later installed.
*   **Maven**: Used for building the project and managing dependencies.
*   **MySQL**: A running MySQL instance is required.
*   **Docker**: (Optional) For running MySQL in a container.

## 2. Database Setup

1.  **Create a MySQL database** for the Parcel Service. Let's name it `parcel_db`.
2.  **Create a user and grant privileges** to the `parcel_db` database.

    ```sql
    CREATE DATABASE parcel_db;
    CREATE USER 'parcel_user'@'localhost' IDENTIFIED BY 'your_password';
    GRANT ALL PRIVILEGES ON parcel_db.* TO 'parcel_user'@'localhost';
    FLUSH PRIVILEGES;
    ```

## 3. Environment Configuration

The primary configuration is in `src/main/resources/application.properties`. You can override these properties using environment variables or a local properties file.

**`application.properties`:**
```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/parcel_db
spring.datasource.username=parcel_user
spring.datasource.password=your_password

# JPA
spring.jpa.hibernate.ddl-auto=update
```

## 4. Build and Run

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

1.  Open the `parcel-service` project in your IDE.
2.  The IDE should automatically detect it as a Maven project and download dependencies.
3.  Find the `ParcelServiceApplication.java` file and run its `main` method.

## 5. Verification

Once the application is running, you can verify it by accessing the health check endpoint (if available) or by calling one of the API endpoints.
