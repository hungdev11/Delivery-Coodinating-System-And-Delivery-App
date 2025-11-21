# Sequence Diagram: Complete Session

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