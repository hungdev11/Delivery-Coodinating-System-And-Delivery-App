# Sequence Diagram: Refuse Parcel

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