### 1.6. Component Diagram: Communication Service

This diagram shows the internal structure of the `communication_service`. It handles real-time communication via WebSockets, sends push notifications, and persists messages.

```mermaid
graph TD
    subgraph "Communication Service"
        direction LR

        subgraph "Application Layer"
            direction TB
            websocket_endpoints["WebSocket Endpoints (@/application/websocket)"]
            rest_controllers["REST Controllers (@/application/rest)"]
            kafka_listeners["Kafka Listeners (@/application/kafka)"]
        end

        subgraph "Business Layer"
            direction TB
            message_handling["Message Handling Logic (@/business)"]
            domain_models["Domain Models (@/business/model)"]
        end

        subgraph "Infrastructure Layer"
            direction TB
            db_repositories["Database Repositories (@/infrastructure/db)"]
            firebase_client["Firebase Client (FCM) (@/infrastructure/firebase)"]
        end

        subgraph "Configuration"
            direction TB
            app_context["Application Context (@/app_context)"]
            security_config["Security Config"]
        end

        %% Connections
        websocket_endpoints -- "Uses" --> message_handling
        rest_controllers -- "Uses" --> message_handling
        kafka_listeners -- "Triggers" --> message_handling

        message_handling -- "Persists/Reads" --> db_repositories
        message_handling -- "Sends Push Notification via" --> firebase_client
        message_handling -- "Uses" --> domain_models

        app_context -- "Configures" --> websocket_endpoints
        app_context -- "Configures" --> security_config
    end

    subgraph "External Systems"
        nginx["Nginx (Reverse Proxy)"]
        kafka["Kafka"]
        database["MySQL Database"]
        firebase["Firebase Cloud Messaging"]
    end

    nginx -- "Forwards WS traffic to" --> websocket_endpoints
    kafka -- "Sends messages to" --> kafka_listeners
    db_repositories -- "Connects to" --> database
    firebase_client -- "Connects to" --> firebase
```
