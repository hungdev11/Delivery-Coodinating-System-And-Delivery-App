**Navigation**: [← Back to Shipper Features](README.md) | [↑ Features Index](../README.md) | [↑ Report Index](../../README.md)

---

# Shipper: Complete Task & Confirm Proof

**Version**: v1  
**Module**: `DeliveryApp/app/src/main/java/com/ds/deliveryapp`  
**Related**: See [Session Service Documentation](../../2_BACKEND/4_SESSION_SERVICE.md) for service details

## Overview

Shipper marks a delivery task as completed after successfully delivering the parcel. This updates assignment status to `COMPLETED` and parcel status to `SUCCEEDED`, notifying the client.

## Activity Diagram

```mermaid
flowchart TD
    A["TaskDetailActivity"] --> B["Driver taps 'Giao thành công'"]
    B --> C["Collect route metrics + photos"]
    C --> D["POST /assignments/drivers/{id}/parcels/{parcelId}/complete"]
    D --> E["Session Service sets assignment=COMPLETED"]
    E --> F["Parcel Service sets parcel=SUCCEEDED"]
    F --> G["Communication Service notifies client"]
```

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Shipper as Shipper (TaskDetailActivity)
    participant Gateway as API Gateway
    participant SessionSvc as Session Service
    participant ParcelSvc as Parcel Service
    participant Kafka as Kafka
    participant CommSvc as Communication Service
    participant Client as Client (WebSocket)
    
    Shipper->>Shipper: Collect delivery proof:<br/>routeInfo, photos (optional)
    Shipper->>Gateway: POST /api/v1/assignments/drivers/{id}/parcels/{parcelId}/complete<br/>{routeInfo: {distance, duration, ...}}
    Gateway->>Gateway: Extract X-User-Id from JWT
    Gateway->>SessionSvc: POST /api/v1/assignments/drivers/{id}/parcels/{parcelId}/complete<br/>Headers: X-User-Id
    
    SessionSvc->>SessionSvc: Validate: assignment exists<br/>Validate: deliveryManId matches<br/>Validate: assignment status = ASSIGNED
    
    SessionSvc->>SessionSvc: Update assignment:<br/>status = COMPLETED<br/>completedAt = now()<br/>routeInfo = request.routeInfo
    
    SessionSvc->>Kafka: Publish AssignmentCompletedEvent<br/>{assignmentId, parcelId, sessionId}
    
    Kafka->>ParcelSvc: AssignmentCompletedEvent
    ParcelSvc->>ParcelSvc: Update parcel:<br/>status = SUCCEEDED<br/>deliveredAt = now()
    
    ParcelSvc->>Kafka: Publish ParcelStatusChangedEvent<br/>{parcelId, status = SUCCEEDED}
    
    Kafka->>CommSvc: ParcelStatusChangedEvent
    CommSvc->>CommSvc: Create notification message<br/>Send to client conversation
    
    CommSvc->>Client: WebSocket: Notification<br/>"Parcel delivered successfully"
    Client->>Client: Show notification<br/>Enable "Confirm Received" button
    
    SessionSvc-->>Gateway: DeliveryAssignment (status = COMPLETED)
    Gateway-->>Shipper: Success response
    
    Shipper->>Shipper: TaskFragment refreshes<br/>Task marked as completed
```

## Code References

- **API**: `SessionClient.completeTask`
- **Activity**: `DeliveryApp/app/src/main/java/com/ds/deliveryapp/TaskDetailActivity.java`
- **Backend**: `BE/session-service/src/main/java/com/ds/session/session_service/application/controllers/DeliveryAssignmentController.java`

## API References

- **Gateway**: `POST /api/v1/assignments/drivers/{id}/parcels/{parcelId}/complete` (see [API Gateway V1 Delivery Assignment Controller](../../3_APIS_AND_FUNCTIONS/apis/api-gateway/v1/V1_DELIVERY_ASSIGNMENT_CONTROLLER.md))
- **Session Service**: V1 controller (see [Session Service V1 Delivery Assignment Controller](../../3_APIS_AND_FUNCTIONS/apis/session-service/v1/SESSION_SERVICE_V1_DELIVERY_ASSIGNMENT_CONTROLLER.md))

## Implementation Notes

- Route info (distance, duration) is collected and stored with assignment
- Photos are optional but recommended for proof
- Client receives WebSocket notification immediately after completion

---

**Navigation**: [← Back to Shipper Features](README.md) | [↑ Features Index](../README.md) | [↑ Report Index](../../README.md)
