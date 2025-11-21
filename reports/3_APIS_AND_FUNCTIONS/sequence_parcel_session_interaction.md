# Sequence Diagram: Parcel and Session State Interaction (Accept & Deliver)

This diagram illustrates how the states of a Parcel and a Session interact during the acceptance and delivery process by a Delivery Man.

```mermaid
sequenceDiagram
    actor DeliveryMan
    participant Client as Client (DeliveryApp)
    participant APIGateway as API Gateway
    participant SessionService as Session Service
    participant ParcelService as Parcel Service

    DeliveryMan->>Client: Accepts Parcel (Parcel A)
    Client->>APIGateway: POST /api/v1/sessions/drivers/{deliveryManId}/accept-parcel (Parcel A)
    APIGateway->>SessionService: Accept Parcel (deliveryManId, Parcel A)
    SessionService->>ParcelService: Update Parcel A Status to PENDING_PICKUP
    ParcelService-->>SessionService: Parcel A Status Updated
    SessionService-->>APIGateway: Parcel A Accepted in Session
    APIGateway-->>Client: Confirmation

    Note over DeliveryMan,Client: ... Time passes, Delivery Man picks up and delivers Parcel A ...

    DeliveryMan->>Client: Marks Parcel A as Delivered
    Client->>APIGateway: PUT /api/v1/parcels/deliver/{parcelId} (Parcel A)
    APIGateway->>ParcelService: Deliver Parcel (Parcel A)
    ParcelService-->>APIGateway: Parcel A Status DELIVERED
    APIGateway-->>Client: Confirmation

    Note over DeliveryMan,Client: ... Delivery Man completes all tasks in session ...

    DeliveryMan->>Client: Completes Session
    Client->>APIGateway: POST /api/v1/sessions/{sessionId}/complete
    APIGateway->>SessionService: Complete Session (sessionId)
    SessionService-->>APIGateway: Session COMPLETED
    APIGateway-->>Client: Confirmation
```