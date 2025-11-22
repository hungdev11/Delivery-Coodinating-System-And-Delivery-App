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
    Client->>APIGateway: POST /api/v1/parcels (parcel details)
    APIGateway->>UserService: Validate User (if needed)
    UserService-->>APIGateway: User Validation Result
    APIGateway->>ZoneService: Validate Addresses (if needed)
    ZoneService-->>APIGateway: Address Validation Result
    APIGateway->>ParcelService: Create Parcel (parcel details)
    ParcelService-->>APIGateway: Parcel Creation Result
    APIGateway-->>Client: Parcel Creation Confirmation/Error
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
    Client->>APIGateway: POST /api/v1/sessions/drivers/{deliveryManId}/accept-parcel (parcelId)
    APIGateway->>SessionService: Accept Parcel (deliveryManId, parcelId)
    SessionService->>ParcelService: Update Parcel Status to "Accepted"
    ParcelService-->>SessionService: Status Update Confirmation
    SessionService-->>APIGateway: Acceptance Result
    APIGateway-->>Client: Acceptance Confirmation/Error
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
    Client->>APIGateway: PUT /api/v1/parcels/deliver/{parcelId}
    APIGateway->>ParcelService: Deliver Parcel (parcelId)
    ParcelService-->>APIGateway: Delivery Result
    APIGateway->>CommunicationService: Send Delivery Notification (optional)
    CommunicationService-->>APIGateway: Notification Result
    APIGateway-->>Client: Delivery Confirmation/Error
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
    Client->>APIGateway: PUT /api/v1/parcels/confirm/{parcelId}
    APIGateway->>ParcelService: Confirm Parcel (parcelId)
    ParcelService-->>APIGateway: Confirmation Result
    APIGateway->>CommunicationService: Send Confirmation Notification (optional)
    CommunicationService-->>APIGateway: Notification Result
    APIGateway-->>Client: Confirmation/Error
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
    Client->>APIGateway: PUT /api/v1/parcels/refuse/{parcelId}
    APIGateway->>ParcelService: Refuse Parcel (parcelId)
    ParcelService-->>APIGateway: Refusal Result
    APIGateway->>CommunicationService: Send Refusal Notification (optional)
    CommunicationService-->>APIGateway: Notification Result
    APIGateway-->>Client: Refusal Confirmation/Error
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
    Client->>APIGateway: PUT /api/v1/parcels/dispute/{parcelId} (dispute details)
    APIGateway->>ParcelService: Dispute Parcel (parcelId, dispute details)
    ParcelService-->>APIGateway: Dispute Result
    APIGateway->>CommunicationService: Send Dispute Notification (optional)
    CommunicationService-->>APIGateway: Notification Result
    APIGateway-->>Client: Dispute Confirmation/Error
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
