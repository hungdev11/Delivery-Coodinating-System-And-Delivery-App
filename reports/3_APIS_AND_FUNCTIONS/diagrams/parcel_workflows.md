# Parcel Workflows

This document contains sequence diagrams and state diagrams for all parcel-related operations in the Delivery System.

## Parcel State Lifecycle

The following diagram illustrates the complete lifecycle and state transitions of a Parcel entity:

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> PENDING_PICKUP: assigned to delivery man
    PENDING_PICKUP --> IN_TRANSIT: picked up
    IN_TRANSIT --> DELIVERED: delivered
    DELIVERED --> CONFIRMED: recipient confirms
    DELIVERED --> DISPUTED: recipient disputes
    IN_TRANSIT --> REFUSED: recipient refuses
    PENDING_PICKUP --> CANCELLED: cancelled by user/admin
    IN_TRANSIT --> CANCELLED: cancelled by user/admin
    REFUSED --> PENDING_PICKUP: re-attempt delivery
    DISPUTED --> RESOLVED: dispute resolved
```

## Create Parcel

This diagram illustrates the sequence of interactions for creating a new parcel in the system.

```mermaid
sequenceDiagram
    actor User
    participant Client as Client (ManagementSystem)
    participant APIGateway as API Gateway
    participant ParcelService as Parcel Service
    participant UserService as User Service
    participant ZoneService as Zone Service

    User->>Client: Enters parcel details
    activate Client
    Client->>APIGateway: POST /api/v1/parcels (parcel details)
    activate APIGateway
    APIGateway->>UserService: Validate User (if needed)
    activate UserService
    UserService-->>APIGateway: User Validation Result
    deactivate UserService
    APIGateway->>ZoneService: Validate Addresses (if needed)
    activate ZoneService
    ZoneService-->>APIGateway: Address Validation Result
    deactivate ZoneService
    APIGateway->>ParcelService: Create Parcel (parcel details)
    activate ParcelService
    ParcelService-->>APIGateway: Parcel Creation Result
    deactivate ParcelService
    deactivate APIGateway
    APIGateway-->>Client: Parcel Creation Confirmation/Error
    deactivate Client
    Client-->>User: Confirmation/Error
```

## Delivery Man Accepts Parcel

This diagram illustrates the sequence of interactions when a Delivery Man accepts a parcel.

```mermaid
sequenceDiagram
    actor DeliveryMan
    participant Client as Client (DeliveryApp)
    participant APIGateway as API Gateway
    participant SessionService as Session Service
    participant ParcelService as Parcel Service

    DeliveryMan->>Client: Selects parcel to accept
    activate Client
    Client->>APIGateway: POST /api/v1/sessions/drivers/{deliveryManId}/accept-parcel (parcelId)
    activate APIGateway
    APIGateway->>SessionService: Accept Parcel (deliveryManId, parcelId)
    activate SessionService
    SessionService->>ParcelService: Update Parcel Status to "Accepted"
    activate ParcelService
    ParcelService-->>SessionService: Status Update Confirmation
    deactivate ParcelService
    deactivate SessionService
    SessionService-->>APIGateway: Acceptance Result
    deactivate APIGateway
    APIGateway-->>Client: Acceptance Confirmation/Error
    deactivate Client
    Client-->>DeliveryMan: Confirmation/Error
```

## Deliver Parcel

This diagram illustrates the sequence of interactions when a Delivery Man delivers a parcel.

```mermaid
sequenceDiagram
    actor DeliveryMan
    participant Client as Client (DeliveryApp)
    participant APIGateway as API Gateway
    participant ParcelService as Parcel Service
    participant CommunicationService as Communication Service

    DeliveryMan->>Client: Marks parcel as delivered
    activate Client
    Client->>APIGateway: PUT /api/v1/parcels/deliver/{parcelId}
    activate APIGateway
    APIGateway->>ParcelService: Deliver Parcel (parcelId)
    activate ParcelService
    ParcelService-->>APIGateway: Delivery Result
    deactivate ParcelService
    APIGateway->>CommunicationService: Send Delivery Notification (optional)
    activate CommunicationService
    CommunicationService-->>APIGateway: Notification Result
    deactivate CommunicationService
    deactivate APIGateway
    APIGateway-->>Client: Delivery Confirmation/Error
    deactivate Client
    Client-->>DeliveryMan: Confirmation/Error
```

## Confirm Parcel

This diagram illustrates the sequence of interactions when a Recipient confirms the receipt of a parcel.

```mermaid
sequenceDiagram
    actor Recipient
    participant Client as Client (e.g., Web/Mobile Link)
    participant APIGateway as API Gateway
    participant ParcelService as Parcel Service
    participant CommunicationService as Communication Service

    Recipient->>Client: Confirms parcel receipt
    activate Client
    Client->>APIGateway: PUT /api/v1/parcels/confirm/{parcelId}
    activate APIGateway
    APIGateway->>ParcelService: Confirm Parcel (parcelId)
    activate ParcelService
    ParcelService-->>APIGateway: Confirmation Result
    deactivate ParcelService
    APIGateway->>CommunicationService: Send Confirmation Notification (optional)
    activate CommunicationService
    CommunicationService-->>APIGateway: Notification Result
    deactivate CommunicationService
    deactivate APIGateway
    APIGateway-->>Client: Confirmation/Error
    deactivate Client
    Client-->>Recipient: Confirmation/Error
```

## Refuse Parcel

This diagram illustrates the sequence of interactions when a Recipient refuses a parcel.

```mermaid
sequenceDiagram
    actor Recipient
    participant Client as Client (e.g., Web/Mobile Link)
    participant APIGateway as API Gateway
    participant ParcelService as Parcel Service
    participant CommunicationService as Communication Service

    Recipient->>Client: Refuses parcel
    activate Client
    Client->>APIGateway: PUT /api/v1/parcels/refuse/{parcelId}
    activate APIGateway
    APIGateway->>ParcelService: Refuse Parcel (parcelId)
    activate ParcelService
    ParcelService-->>APIGateway: Refusal Result
    deactivate ParcelService
    APIGateway->>CommunicationService: Send Refusal Notification (optional)
    activate CommunicationService
    CommunicationService-->>APIGateway: Notification Result
    deactivate CommunicationService
    deactivate APIGateway
    APIGateway-->>Client: Refusal Confirmation/Error
    deactivate Client
    Client-->>Recipient: Confirmation/Error
```

## Dispute Parcel

This diagram illustrates the sequence of interactions when a User initiates a dispute for a parcel.

```mermaid
sequenceDiagram
    actor User
    participant Client as Client (e.g., Web/Mobile App)
    participant APIGateway as API Gateway
    participant ParcelService as Parcel Service
    participant CommunicationService as Communication Service

    User->>Client: Initiates a dispute for a parcel
    activate Client
    Client->>APIGateway: PUT /api/v1/parcels/dispute/{parcelId} (dispute details)
    activate APIGateway
    APIGateway->>ParcelService: Dispute Parcel (parcelId, dispute details)
    activate ParcelService
    ParcelService-->>APIGateway: Dispute Result
    deactivate ParcelService
    APIGateway->>CommunicationService: Send Dispute Notification (optional)
    activate CommunicationService
    CommunicationService-->>APIGateway: Notification Result
    deactivate CommunicationService
    deactivate APIGateway
    APIGateway-->>Client: Dispute Confirmation/Error
    deactivate Client
    Client-->>User: Confirmation/Error
```

## Parcel and Session State Interaction

This diagram illustrates how the states of a Parcel and a Session interact during the acceptance and delivery process by a Delivery Man.

```mermaid
sequenceDiagram
    actor DeliveryMan
    participant Client as Client (DeliveryApp)
    participant APIGateway as API Gateway
    participant SessionService as Session Service
    participant ParcelService as Parcel Service

    DeliveryMan->>Client: Accepts Parcel (Parcel A)
    activate Client
    Client->>APIGateway: POST /api/v1/sessions/drivers/{deliveryManId}/accept-parcel (Parcel A)
    activate APIGateway
    APIGateway->>SessionService: Accept Parcel (deliveryManId, Parcel A)
    activate SessionService
    SessionService->>ParcelService: Update Parcel A Status to PENDING_PICKUP
    activate ParcelService
    ParcelService-->>SessionService: Parcel A Status Updated
    deactivate ParcelService
    deactivate SessionService
    SessionService-->>APIGateway: Parcel A Accepted in Session
    deactivate APIGateway
    APIGateway-->>Client: Confirmation
    deactivate Client

    Note over DeliveryMan,Client: ... Time passes, Delivery Man picks up and delivers Parcel A ...

    DeliveryMan->>Client: Marks Parcel A as Delivered
    activate Client
    Client->>APIGateway: PUT /api/v1/parcels/deliver/{parcelId} (Parcel A)
    activate APIGateway
    APIGateway->>ParcelService: Deliver Parcel (Parcel A)
    activate ParcelService
    ParcelService-->>APIGateway: Parcel A Status DELIVERED
    deactivate ParcelService
    deactivate APIGateway
    APIGateway-->>Client: Confirmation
    deactivate Client

    Note over DeliveryMan,Client: ... Delivery Man completes all tasks in session ...

    DeliveryMan->>Client: Completes Session
    activate Client
    Client->>APIGateway: POST /api/v1/sessions/{sessionId}/complete
    activate APIGateway
    APIGateway->>SessionService: Complete Session (sessionId)
    activate SessionService
    SessionService-->>APIGateway: Session COMPLETED
    deactivate SessionService
    deactivate APIGateway
    APIGateway-->>Client: Confirmation
    deactivate Client
```
