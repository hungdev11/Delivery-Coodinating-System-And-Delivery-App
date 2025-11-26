**Navigation**: [← Back to Shipper Features](README.md) | [↑ Features Index](../README.md) | [↑ Report Index](../../README.md)

---

# Shipper: Scan & Accept Parcel

**Version**: v0/v1  
**Module**: `DeliveryApp/app/src/main/java/com/ds/deliveryapp`  
**Related**: See [Delivery App Documentation](../../1_CLIENTS/2_DELIVERY_APP.md) for app structure

## Overview

Shipper scans QR code on parcel to accept it into their delivery session. This creates or adds to an active session and marks parcel as `ON_ROUTE`.

## Activity Diagram

```mermaid
flowchart TD
    A["Driver taps Scan in TaskFragment"] --> B["QrScanActivity opens camera"]
    B --> C{"QR decoded?"}
    C -- "No" --> B
    C -- "Yes" --> D["Send POST /sessions/drivers/{id}/accept-parcel"]
    D --> E["Session Service returns assignmentId"]
    E --> F["ParcelService updates parcel=ON_ROUTE"]
    F --> G["TaskFragment refreshes assignments list"]
    G --> H["MapFragment fetches route from Zone Service (optional)"]
```

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Shipper as Shipper (QrScanActivity)
    participant Gateway as API Gateway
    participant SessionSvc as Session Service
    participant ParcelSvc as Parcel Service
    participant Kafka as Kafka
    participant Client as Client (WebSocket)
    
    activate Shipper
    Shipper->>Shipper: Scan QR code (parcel code)
    Shipper->>Gateway: POST /api/v1/sessions/drivers/{id}/accept-parcel<br/>{parcelCode: "ABC123"}
    activate Gateway
    Gateway->>Gateway: Extract X-User-Id from JWT
    Gateway->>SessionSvc: POST /api/v1/sessions/drivers/{id}/accept-parcel<br/>Headers: X-User-Id
    activate SessionSvc
    SessionSvc->>ParcelSvc: GET /api/v1/parcels/code/{code}
    activate ParcelSvc
    ParcelSvc-->>SessionSvc: Parcel (id, status, receiverId)
    deactivate ParcelSvc
    
    alt Active session exists
        SessionSvc->>SessionSvc: Add assignment to existing session
    else No active session
        SessionSvc->>SessionSvc: Create new session (CREATED)<br/>Add assignment
    end
    
    SessionSvc->>SessionSvc: Create DeliveryAssignment:<br/>parcelId, sessionId, deliveryManId<br/>status = ASSIGNED
    SessionSvc->>Kafka: Publish AssignmentCreatedEvent
    activate Kafka
    Kafka->>ParcelSvc: AssignmentCreatedEvent
    activate ParcelSvc
    ParcelSvc->>ParcelSvc: Update parcel:<br/>status = ON_ROUTE
    ParcelSvc->>Kafka: Publish ParcelStatusChangedEvent
    deactivate ParcelSvc
    Kafka->>Client: WebSocket notification
    activate Client
    Client->>Client: Refresh parcel list
    deactivate Client
    deactivate Kafka
    deactivate SessionSvc
    SessionSvc-->>Gateway: DeliveryAssignment (assignmentId, sessionId)
    deactivate Gateway
    Gateway-->>Shipper: Success response
    deactivate Shipper
    Shipper->>Shipper: TaskFragment refreshes<br/>MapFragment loads route (optional)
```

## Code References

- **API**: `SessionClient.acceptParcelToSession`
- **Activity**: `DeliveryApp/app/src/main/java/com/ds/deliveryapp/QrScanActivity.java`
- **Fragment**: `DeliveryApp/app/src/main/java/com/ds/deliveryapp/TaskFragment.java`
- **Backend**: `BE/session-service/src/main/java/com/ds/session/session_service/application/controllers/SessionController.java`

## Implementation Notes

- `TaskFragment` should call `SessionDashboardFragment#createSession` if no active session exists
- Route fetching from Zone Service is optional and can be done asynchronously
- QR code contains parcel code, not parcel ID

## API References

- **Gateway**: `POST /api/v1/sessions/drivers/{id}/accept-parcel` (see [API Gateway V1 Session Controller](../../3_APIS_AND_FUNCTIONS/apis/api-gateway/v1/V1_SESSION_CONTROLLER.md))
- **Session Service**: V1 controller (see [Session Service V1 Session Controller](../../3_APIS_AND_FUNCTIONS/apis/session-service/v1/SESSION_SERVICE_V1_SESSION_CONTROLLER.md))

---

**Navigation**: [← Back to Shipper Features](README.md) | [↑ Features Index](../README.md) | [↑ Report Index](../../README.md)
