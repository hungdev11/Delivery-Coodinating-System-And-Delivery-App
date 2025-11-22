# Settings Service

This document describes the Settings Service, which provides centralized configuration management for the Delivery System.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Component Structure](#component-structure)
- [Package Structure](#package-structure)
- [Key Components](#key-components)
- [Functionality](#functionality)
- [Technical Details](#technical-details)
- [Related Documentation](#related-documentation)

## Overview

The Settings Service provides centralized configuration management for the Delivery System. It allows administrators to update system settings without modifying individual service configurations or restarting services.

## Architecture

The service follows a simple CRUD (Create, Read, Update, Delete) pattern with REST controllers handling API requests and business logic managing setting storage and retrieval.

## Component Structure

The following diagram illustrates the internal structure of the Settings Service:

```mermaid
graph TD
    subgraph "Settings Service"
        direction LR

        subgraph "Application Layer"
            direction TB
            rest_controllers["REST Controllers (@/application)"]
            openapi_docs["OpenAPI Docs (SpringDoc)"]
        end

        subgraph "Business Layer"
            direction TB
            settings_logic["Settings Management Logic (@/business)"]
            domain_models["Domain Models (@/business/model)"]
            cache["Cache Abstraction"]
        end

        subgraph "Infrastructure Layer"
            direction TB
            db_repositories["Database Repositories"]
            flyway_migrations["Flyway Migrations"]
        end

        subgraph "Configuration"
            direction TB
            app_context["Application Context (@/app_context)"]
            config["App Config (@/config)"]
        end

        %% Connections
        rest_controllers -- "Uses" --> settings_logic
        settings_logic -- "Uses" --> cache
        settings_logic -- "Persists/Reads" --> db_repositories
        settings_logic -- "Uses" --> domain_models
        
        flyway_migrations -- "Manages Schema for" --> db_repositories
        rest_controllers -- "Generates" --> openapi_docs
        
        app_context -- "Loads" --> config
    end

    subgraph "External Systems"
        api_gateway["API Gateway"]
        database["MySQL Database"]
    end

    api_gateway -- "Routes requests to" --> rest_controllers
    db_repositories -- "Connects to" --> database
```

## Package Structure

The service is organized into the following packages:

```mermaid
flowchart TB
    subgraph SettingsService["Settings Service"]
        SpringBootApp["Spring Boot Application (src, pom.xml)"]
        Dockerfile["Dockerfile"]
    end
```

## Key Components

**Settings Management**: Business logic handles setting creation, retrieval, updates, and deletion. Settings are organized into groups and identified by keys within those groups.

**Caching**: The service uses caching to improve performance for frequently accessed settings, reducing database load and response times.

**Database Migrations**: Schema changes are managed through Flyway migrations, ensuring consistent database structure across deployments.

**API Documentation**: The service generates OpenAPI documentation automatically, providing clear API specifications for integration.

## Functionality

**Setting Storage**: Settings are stored in a database with support for grouping and hierarchical organization. This allows logical organization of related settings.

**Bulk Operations**: The service supports bulk updates, allowing administrators to update multiple settings in a single operation for efficiency.

**Value Retrieval**: Settings can be retrieved individually by group and key, or as complete groups. This flexibility supports different access patterns.

**Health Monitoring**: The service provides health check endpoints that can be used by monitoring systems to verify service availability.

## Technical Details

The service is built using Spring Boot with JPA for database operations. Caching is implemented using Spring Cache abstraction, which can be configured to use various caching providers. The service uses SpringDoc to automatically generate OpenAPI documentation from code annotations. Database migrations are managed using Flyway to ensure schema consistency across environments.

For detailed API endpoint documentation, see [Settings Service API Documentation](../../3_APIS_AND_FUNCTIONS/apis/settings-service/README.md).

## Related Documentation

- [System Overview](../0_SYSTEM_OVERVIEW.md) - High-level system architecture
- [API Gateway](1_API_GATEWAY.md) - Entry point and routing layer
- [System Analysis](../SYSTEM_ANALYSIS.md) - System analysis and technical assessment
