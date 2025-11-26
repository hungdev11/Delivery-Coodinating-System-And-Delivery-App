# System Architecture Flows

This document contains sequence diagrams for system-level operations including authentication and data flow patterns.

## User Login

This diagram illustrates the sequence of interactions for a user logging into the system.

```mermaid
sequenceDiagram
    actor User
    participant Client as Client (DeliveryApp/ManagementSystem)
    participant APIGateway as API Gateway
    participant UserService as User Service

    User->>Client: Enters credentials
    activate Client
    Client->>APIGateway: POST /api/v1/auth/login (credentials)
    activate APIGateway
    APIGateway->>UserService: Authenticate User (credentials)
    activate UserService
    UserService-->>APIGateway: Authentication Result (Token/Error)
    deactivate UserService
    deactivate APIGateway
    APIGateway-->>Client: Authentication Result (Token/Error)
    deactivate Client
    Client-->>User: Login Success/Failure
```

## High-Level Layer Data Flow

This diagram illustrates the typical data flow between the main architectural layers of the system for a user-initiated action.

```mermaid
sequenceDiagram
    actor User
    participant Client as Client (Web/Mobile)
    participant Nginx as Nginx (Reverse Proxy)
    participant APIGateway as API Gateway
    participant Microservice as Microservice (e.g., User Service)
    participant Database as Database

    User->>Client: Initiates Action
    activate Client
    Client->>Nginx: HTTP Request
    activate Nginx
    Nginx->>APIGateway: Forward Request
    activate APIGateway
    APIGateway->>Microservice: API Call
    activate Microservice
    Microservice->>Database: Data Access
    activate Database
    Database-->>Microservice: Data Response
    deactivate Database
    deactivate Microservice
    Microservice-->>APIGateway: API Response
    deactivate APIGateway
    APIGateway-->>Nginx: Forward Response
    deactivate Nginx
    Nginx-->>Client: HTTP Response
    deactivate Client
    Client-->>User: Displays Result
```
