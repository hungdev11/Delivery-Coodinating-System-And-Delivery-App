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
    Client->>APIGateway: POST /api/v1/auth/login (credentials)
    APIGateway->>UserService: Authenticate User (credentials)
    UserService-->>APIGateway: Authentication Result (Token/Error)
    APIGateway-->>Client: Authentication Result (Token/Error)
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
    Client->>Nginx: HTTP Request
    Nginx->>APIGateway: Forward Request
    APIGateway->>Microservice: API Call
    Microservice->>Database: Data Access
    Database-->>Microservice: Data Response
    Microservice-->>APIGateway: API Response
    APIGateway-->>Nginx: Forward Response
    Nginx-->>Client: HTTP Response
    Client-->>User: Displays Result
```
