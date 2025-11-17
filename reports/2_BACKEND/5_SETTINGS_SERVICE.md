### 1.9. Component Diagram: Settings Service

This diagram shows the internal structure of the `Settings_service`. It is a CRUD service for managing application settings, with support for caching and database migrations.

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
