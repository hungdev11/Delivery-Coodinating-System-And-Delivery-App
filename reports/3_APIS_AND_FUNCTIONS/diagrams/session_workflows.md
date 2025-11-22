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
    Client->>APIGateway: POST /api/v1/sessions/drivers/{deliveryManId}/prepare
    APIGateway->>UserService: Validate DeliveryMan (if needed)
    UserService-->>APIGateway: Validation Result
    APIGateway->>SessionService: Prepare Session for DeliveryMan (deliveryManId)
    SessionService-->>APIGateway: Session Preparation Result (sessionId)
    APIGateway-->>Client: Session Preparation Confirmation (sessionId)
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
    Client->>APIGateway: POST /api/v1/sessions/{sessionId}/start
    APIGateway->>SessionService: Start Session (sessionId)
    SessionService-->>APIGateway: Session Start Result
    APIGateway-->>Client: Session Start Confirmation
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
    Client->>APIGateway: POST /api/v1/sessions/{sessionId}/complete
    APIGateway->>SessionService: Complete Session (sessionId)
    SessionService-->>APIGateway: Session Completion Result
    APIGateway-->>Client: Session Completion Confirmation
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
    Client->>APIGateway: POST /api/v1/sessions/{sessionId}/fail
    APIGateway->>SessionService: Fail Session (sessionId)
    SessionService-->>APIGateway: Session Failure Result
    APIGateway-->>Client: Session Failure Confirmation
    Client-->>DeliveryMan: Confirmation
```
