# Settings Service Architecture

## 1. Overview

The Settings Service is a standard Spring Boot application that provides a centralized system for managing application and service settings. It allows for the dynamic configuration of various parts of the system without requiring code changes or redeployments.

## 2. Core Technology

*   **Spring Boot**: The underlying application framework.
*   **Spring Data JPA**: For database interaction with a MySQL database.
*   **Flyway**: For database schema migrations.
*   **Spring Cache**: For caching frequently accessed settings to improve performance.
*   **Springdoc OpenAPI**: For automatic API documentation generation.

## 3. Project Structure

```
/src/main/java/com/ds/setting/
├── app_context/                # Data access layer
│   ├── models/                 # JPA entities (e.g., Setting.java)
│   └── repositories/           # JPA repositories (e.g., SettingRepository.java)
├── application/                # Web and configuration layer
│   ├── configs/                # Spring configurations
│   ├── controllers/            # REST API controllers
│   └── startup/                # Application startup logic
├── business/                   # Business logic layer
│   └── v1/
│       └── services/           # Service implementations
├── common/                     # Shared components
│   └── entities/               # DTOs and common models
└── SettingsServiceApplication.java # Spring Boot main application class
```

## 4. Database Schema

The core of the service is the `settings` table in the MySQL database. A simplified schema would look like this:

```sql
CREATE TABLE settings (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `setting_group` VARCHAR(255) NOT NULL, -- e.g., 'user-service', 'global'
  `setting_key` VARCHAR(255) NOT NULL,   -- e.g., 'max-login-attempts'
  `setting_value` TEXT NOT NULL,
  `description` VARCHAR(1000),
  `type` VARCHAR(50), -- e.g., 'STRING', 'INTEGER', 'BOOLEAN'
  `is_editable` BOOLEAN DEFAULT TRUE,
  UNIQUE KEY `uk_group_key` (`setting_group`, `setting_key`)
);
```

*   Settings are uniquely identified by a combination of their `group` and `key`.

## 5. Caching

The service uses Spring's caching abstraction (`@EnableCaching`) to improve performance. 

*   `@Cacheable`: Frequently requested settings (e.g., by group and key) are cached to avoid repeated database lookups.
*   `@CacheEvict`: When a setting is updated or deleted, the corresponding cache entry is evicted to ensure data consistency.

This significantly reduces database load and improves response times for settings lookups.

## 6. API Documentation

The service uses `springdoc-openapi` to automatically generate interactive API documentation (Swagger UI). This documentation is available at `/swagger-ui.html` when the service is running.
