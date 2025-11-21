### 1.5. Component Diagram: API Gateway

This diagram shows the internal structure of the `api-gateway` service. It acts as a reverse proxy, handling security, routing, and some orchestration. It appears to follow a hexagonal architecture.

```mermaid
graph TD
    subgraph "API Gateway"
        direction LR

        subgraph "Inbound (Application Layer)"
            direction TB
            route_config["Route Configuration (@/application)"]
            filters["Gateway Filters (@/application/filters)"]
            controllers["Controllers (@/application/controllers)"]
        end

        subgraph "Core Logic (Business Layer)"
            direction TB
            orchestration["Orchestration Logic (@/business)"]
            domain_models["Domain Models (@/business/model)"]
        end

        subgraph "Outbound (Infrastructure Layer)"
            direction TB
            security["Security (Keycloak, JWT) (@/infrastructure/security)"]
            http_clients["Microservice Clients (@/infrastructure/clients)"]
            kafka_producer["Kafka Producer (@/infrastructure/kafka)"]
        end

        subgraph "Cross-Cutting"
            direction TB
            annotations["Annotations (@/annotations)"]
            common["Common Utilities (@/common)"]
        end

        %% Connections
        controllers -- "Uses" --> orchestration
        route_config -- "Applies" --> filters
        filters -- "Use" --> security
        orchestration -- "Uses" --> http_client
        orchestration -- "Uses" --> domain_models
        
        controllers -- "Sends event to" --> kafka_producer
        filters -- "Sends event to" --> kafka_producer

        security -- "Authenticates/Authorizes" --> controllers
    end

    subgraph "External Systems"
        Microservices
        Keycloak
        Kafka
    end

    http_clients["Microservice Clients"] --> Microservices
    security --> Keycloak
    kafka_producer --> Kafka
```
