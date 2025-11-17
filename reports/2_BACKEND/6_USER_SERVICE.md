### 1.10. Component Diagram: User Service

This diagram shows the internal structure of the `User_service`. It is responsible for all user-related operations, including management in both its local database and the Keycloak identity provider.

```mermaid
graph TD
    subgraph "User Service"
        direction LR

        subgraph "Application Layer"
            direction TB
            rest_controllers["REST Controllers (@/application/rest)"]
            kafka_handlers["Kafka Handlers (@/application/kafka)"]
            openapi_docs["OpenAPI Docs (SpringDoc)"]
        end

        subgraph "Business Layer"
            direction TB
            user_logic["User Management Logic (@/business)"]
            domain_models["Domain Models (@/business/model)"]
        end

        subgraph "Infrastructure Layer"
            direction TB
            db_repositories["Database Repositories (@/infrastructure/db)"]
            keycloak_client["Keycloak Admin Client (@/infrastructure/keycloak)"]
            flyway_migrations["Flyway Migrations"]
        end

        subgraph "Configuration"
            direction TB
            app_context["Application Context (@/app_context)"]
            security_config["Security Config"]
        end

        %% Connections
        rest_controllers -- "Uses" --> user_logic
        kafka_handlers -- "Interact with" --> user_logic

        user_logic -- "Persists/Reads" --> db_repositories
        user_logic -- "Manages users in Keycloak via" --> keycloak_client
        user_logic -- "Uses" --> domain_models
        
        flyway_migrations -- "Manages Schema for" --> db_repositories
        rest_controllers -- "Generates" --> openapi_docs
        
        app_context -- "Configures" --> security_config
    end

    subgraph "External Systems"
        api_gateway["API Gateway"]
        kafka["Kafka"]
        database["MySQL Database"]
        keycloak["Keycloak"]
    end

    api_gateway -- "Routes requests to" --> rest_controllers
    kafka <--> kafka_handlers
    db_repositories -- "Connects to" --> database
    keycloak_client -- "Connects to" --> keycloak
```
