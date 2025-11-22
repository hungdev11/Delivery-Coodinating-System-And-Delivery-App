**Navigation**: [ Back to admin Features](README.md) | [ Features Index](../README.md) | [ Report Index](../../README.md)

---

# Admin: Confirm Delivery Completion

**Version**: v1  
**Module**: `ManagementSystem/src/modules/Parcels`  
**Related**: See `../../SYSTEM_ANALYSIS.md` for system analysis

## Overview

Admin confirms delivery completion after shipper has marked parcel as successfully delivered. This flow updates parcel status to `DELIVERED` and marks the corresponding assignment as `SUCCESS`.

## Activity Diagram

```mermaid
flowchart TD
    A["Parcel list in ManagementSystem"] -->|"Select parcel"| B["Open parcel detail drawer"]
    B --> C{"Parcel status == SUCCEEDED?"}
    C -- "No" --> D["Wait for shipper update via Communication Service"]
    D -->|"Receive event"| C
    C -- "Yes" --> E["Admin clicks Confirm Receipt"]
    E --> F["Call API Gateway PUT /v1/parcels/{id}/confirm-delivery"]
    F --> G["Parcel Service sets parcel=DELIVERED"]
    G --> H["Session Service updates assignment=SUCCESS"]
    H --> I["Communication Service broadcasts notification"]
    I --> J["ManagementSystem refreshes list + audit log"]
```

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Admin as Admin (ParcelsView)
    participant Gateway as API Gateway
    participant ParcelSvc as Parcel Service
    participant Snapshot as AssignmentSnapshot
    participant SessionSvc as Session Service
    participant Kafka as Kafka
    participant Client as Client (WebSocket)
    
    Note over Admin,Client: Shipper has completed delivery<br/>Parcel status = SUCCEEDED
    
    Admin->>Gateway: PUT /api/v1/parcels/{id}/confirm-delivery<br/>(Admin role required)
    Gateway->>Gateway: Validate admin role from JWT
    Gateway->>ParcelSvc: PUT /api/v1/parcels/{id}/confirm-delivery<br/>Headers: X-User-Id, X-User-Roles
    
    ParcelSvc->>ParcelSvc: Validate: status = SUCCEEDED<br/>Validate: admin role
    
    ParcelSvc->>Snapshot: getOrFetch(parcelId)
    alt Snapshot exists
        Snapshot-->>ParcelSvc: AssignmentSnapshot<br/>(assignmentId, sessionId)
    else Snapshot missing
        ParcelSvc->>SessionSvc: GET /assignments/latest/{parcelId}
        SessionSvc-->>ParcelSvc: LatestAssignmentInfo
        ParcelSvc->>Snapshot: save(AssignmentSnapshot)
    end
    
    ParcelSvc->>ParcelSvc: Update parcel:<br/>status = DELIVERED<br/>confirmedAt = now()<br/>confirmedBy = adminId
    
    ParcelSvc->>SessionSvc: PUT /sessions/{sessionId}/assignments/{assignmentId}/success
    SessionSvc->>SessionSvc: Update assignment status = SUCCESS
    SessionSvc-->>ParcelSvc: Success
    
    ParcelSvc->>Kafka: Publish ParcelStatusChangedEvent<br/>(status = DELIVERED)
    Kafka->>Client: WebSocket notification
    Client->>Client: Refresh parcel list
    
    ParcelSvc-->>Gateway: ParcelResponse (status = DELIVERED)
    Gateway-->>Admin: Success response
    Admin->>Admin: Refresh parcel list
```

## Implementation Notes

- **UI hook**: Add action button to `ParcelsView.vue` (admin scope) and to chat sidebar when conversing with the shipper (proposal confirmation).
- **Backend**: Expose `PUT /v1/parcels/{id}/confirm-delivery` in Parcel Service + propagate assignment update through Session Service.
- **Audit**: Reuse `AUDIT_LOGGING_GUIDE.md` patterns (BE).
- **Cross-service communication**: Uses `AssignmentSnapshot` for efficient cross-service communication (same pattern as client confirmation).

## API References

- **Gateway**: `PUT /api/v1/parcels/{id}/confirm-delivery` (see [API Gateway V1 PARCEL_SERVICE_CONTROLLER Controller](../../3_APIS_AND_FUNCTIONS/apis/api-gateway/v1/V1_PARCEL_SERVICE_CONTROLLER.md))
- **Parcel Service**: `PUT /api/v1/parcels/{id}/confirm-delivery` (see `reports/2_BACKEND/3_PARCEL_SERVICE.md`)
- **Session Service**: `PUT /api/v1/sessions/{sessionId}/assignments/{assignmentId}/success` (see `reports/2_BACKEND/4_SESSION_SERVICE.md`)

## Code References

- **Frontend**: `ManagementSystem/src/modules/Parcels/ParcelsView.vue`
- **Backend**: `BE/parcel-service/src/main/java/com/ds/parcel_service/application/controllers/ParcelController.java`
- **Business Logic**: `BE/parcel-service/src/main/java/com/ds/parcel_service/business/v1/services/ParcelService.java`

## Known Issues

- Currently missing UI button for admin confirmation (see `../../SYSTEM_ANALYSIS.md` section 4)
- Backend endpoint exists but needs UI integration


---

**Navigation**: [ Back to admin Features](README.md) | [ Features Index](../README.md) | [ Report Index](../../README.md)