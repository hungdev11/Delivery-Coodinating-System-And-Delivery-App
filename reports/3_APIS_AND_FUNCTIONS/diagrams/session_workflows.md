# Session Workflows

This document contains sequence diagrams and state diagrams for all delivery session-related operations in the Delivery System.

## Session State Lifecycle

The following diagram illustrates the complete lifecycle and state transitions of a Session entity:

```mermaid
stateDiagram-v2
    [*] --> PREPARED
    PREPARED --> ACTIVE: start session
    ACTIVE --> COMPLETED: complete session
    ACTIVE --> FAILED: fail session
    PREPARED --> CANCELLED: cancel session
    ACTIVE --> CANCELLED: cancel session
```

## Prepare Session

This diagram illustrates the sequence of interactions when a Delivery Man prepares for a session.

```mermaid
sequenceDiagram
    actor DeliveryMan
    participant Client as Client (DeliveryApp)
    participant APIGateway as API Gateway
    participant SessionService as Session Service
    participant UserService as User Service

    DeliveryMan->>Client: Requests to prepare for session
    activate Client
    Client->>APIGateway: POST /api/v1/sessions/drivers/{deliveryManId}/prepare
    activate APIGateway
    APIGateway->>UserService: Validate DeliveryMan (if needed)
    activate UserService
    UserService-->>APIGateway: Validation Result
    deactivate UserService
    APIGateway->>SessionService: Prepare Session for DeliveryMan (deliveryManId)
    activate SessionService
    SessionService-->>APIGateway: Session Preparation Result (sessionId)
    deactivate SessionService
    deactivate APIGateway
    APIGateway-->>Client: Session Preparation Confirmation (sessionId)
    deactivate Client
    Client-->>DeliveryMan: Confirmation
```

## Start Session

This diagram illustrates the sequence of interactions when a Delivery Man starts a session.

```mermaid
sequenceDiagram
    actor DeliveryMan
    participant Client as Client (DeliveryApp)
    participant APIGateway as API Gateway
    participant SessionService as Session Service

    DeliveryMan->>Client: Requests to start session
    activate Client
    Client->>APIGateway: POST /api/v1/sessions/{sessionId}/start
    activate APIGateway
    APIGateway->>SessionService: Start Session (sessionId)
    activate SessionService
    SessionService-->>APIGateway: Session Start Result
    deactivate SessionService
    deactivate APIGateway
    APIGateway-->>Client: Session Start Confirmation
    deactivate Client
    Client-->>DeliveryMan: Confirmation
```

## Complete Session

This diagram illustrates the sequence of interactions when a Delivery Man completes a session.

```mermaid
sequenceDiagram
    actor DeliveryMan
    participant Client as Client (DeliveryApp)
    participant APIGateway as API Gateway
    participant SessionService as Session Service

    DeliveryMan->>Client: Requests to complete session
    activate Client
    Client->>APIGateway: POST /api/v1/sessions/{sessionId}/complete
    activate APIGateway
    APIGateway->>SessionService: Complete Session (sessionId)
    activate SessionService
    SessionService-->>APIGateway: Session Completion Result
    deactivate SessionService
    deactivate APIGateway
    APIGateway-->>Client: Session Completion Confirmation
    deactivate Client
    Client-->>DeliveryMan: Confirmation
```

## Fail Session

This diagram illustrates the sequence of interactions when a Delivery Man reports a session failure.

```mermaid
sequenceDiagram
    actor DeliveryMan
    participant Client as Client (DeliveryApp)
    participant APIGateway as API Gateway
    participant SessionService as Session Service

    DeliveryMan->>Client: Reports session failure
    activate Client
    Client->>APIGateway: POST /api/v1/sessions/{sessionId}/fail
    activate APIGateway
    APIGateway->>SessionService: Fail Session (sessionId)
    activate SessionService
    SessionService-->>APIGateway: Session Failure Result
    deactivate SessionService
    deactivate APIGateway
    APIGateway-->>Client: Session Failure Confirmation
    deactivate Client
    Client-->>DeliveryMan: Confirmation
```
