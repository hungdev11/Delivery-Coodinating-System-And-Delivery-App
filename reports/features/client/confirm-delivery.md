**Navigation**: [ Back to client Features](README.md) | [ Features Index](../README.md) | [ Report Index](../../README.md)

---

# Client: Confirm Delivery & Dispute

**Version**: v1  
**Module**: `ManagementSystem/src/modules/Client`  
**Related**: See `../../SYSTEM_ANALYSIS.md` for system analysis

## Overview

Client confirms receipt of parcel after shipper has completed delivery. This updates parcel status to `SUCCEEDED` and marks assignment as `SUCCESS`.

## Activity Diagram

```mermaid
flowchart TD
    A["MyParcelsView shows parcel list"] --> B{"Parcel status = DELIVERED?"}
    B -- "No" --> C["Wait for shipper to complete delivery"]
    C -->|"Shipper completes"| D["Parcel status = DELIVERED"]
    D --> B
    B -- "Yes" --> E["Client taps Confirm Received button"]
    E --> F["POST /api/v1/client/parcels/{id}/confirm"]
    F --> G["Parcel Service validates receiver + status"]
    G --> H["Parcel Service uses AssignmentSnapshot"]
    H --> I["Update parcel status = SUCCEEDED"]
    I --> J["Update Session Service assignment = SUCCESS"]
    J --> K["Client sees success banner"]
```

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Client as Client (MyParcelsView)
    participant Gateway as API Gateway
    participant ParcelSvc as Parcel Service
    participant Snapshot as AssignmentSnapshot
    participant SessionSvc as Session Service
    
    Note over Client,SessionSvc: Shipper has already completed delivery<br/>Parcel status = DELIVERED
    
    Client->>Gateway: POST /api/v1/client/parcels/{id}/confirm<br/>{confirmationCode?, note?}
    Gateway->>Gateway: Extract X-User-Id from JWT
    Gateway->>ParcelSvc: POST /api/v1/client/parcels/{id}/confirm<br/>Headers: X-User-Id, X-User-Roles
    
    ParcelSvc->>ParcelSvc: Validate: user is receiver<br/>Validate: status = DELIVERED
    
    ParcelSvc->>Snapshot: getOrFetch(parcelId)
    alt Snapshot exists
        Snapshot-->>ParcelSvc: AssignmentSnapshot<br/>(assignmentId, sessionId)
    else Snapshot missing
        ParcelSvc->>SessionSvc: GET /assignments/latest/{parcelId}
        SessionSvc-->>ParcelSvc: LatestAssignmentInfo
        ParcelSvc->>Snapshot: save(AssignmentSnapshot)
        Snapshot-->>ParcelSvc: AssignmentSnapshot
    end
    
    ParcelSvc->>ParcelSvc: Update parcel:<br/>status = SUCCEEDED<br/>confirmedAt = now()<br/>confirmedBy = userId<br/>confirmationNote = note
    
    ParcelSvc->>SessionSvc: PUT /sessions/{sessionId}/assignments/{assignmentId}/success<br/>{note}
    SessionSvc->>SessionSvc: Update assignment status = SUCCESS
    SessionSvc-->>ParcelSvc: Success
    
    ParcelSvc->>Snapshot: updateStatus(assignmentId, "SUCCESS")
    
    ParcelSvc-->>Gateway: ParcelResponse (status = SUCCEEDED)
    Gateway-->>Client: Success response
    
    Note over Client,SessionSvc: Client sees confirmation success<br/>Parcel list refreshes automatically
```

## Implementation Details

**Client-scoped parcel API** (`/api/v1/client/parcels/*`):
- Automatically filters parcels by `receiverId` from JWT (`X-User-Id` header)
- Enforces authorization: clients can only view/confirm their own received parcels
- Uses V2 paging/filtering for advanced queries

**Snapshot strategy for cross-service queries**:
- `AssignmentSnapshot` entity in Parcel Service caches assignment metadata locally
- Reduces synchronous calls to Session Service during confirmation flow
- Snapshot is populated via:
  1. Initial fetch from Session Service when needed
  2. Future: Kafka events from Session Service (to be implemented)
- Used in `confirmParcelByClient()` to retrieve `assignmentId` and `sessionId` without blocking on Session Service

## Code References

- **Frontend**: `ManagementSystem/src/modules/Client/MyParcelsView.vue`
- **API Gateway**: `BE/api-gateway/src/main/java/com/ds/gateway/application/controllers/v1/ClientParcelController.java`
- **Parcel Service**: `BE/parcel-service/src/main/java/com/ds/parcel_service/application/controllers/client/ClientParcelController.java`
- **Business Logic**: `BE/parcel-service/src/main/java/com/ds/parcel_service/business/v1/services/ParcelService.java#confirmParcelByClient`
- **Snapshot Service**: `BE/parcel-service/src/main/java/com/ds/parcel_service/application/services/AssignmentSnapshotService.java`

## API References

- **Gateway**: `POST /api/v1/client/parcels/{id}/confirm` (see [API Gateway V1 CLIENT_DELIVERY_SESSION_CONTROLLER Controller](../../3_APIS_AND_FUNCTIONS/apis/api-gateway/v1/V1_CLIENT_DELIVERY_SESSION_CONTROLLER.md))

## Backlog

- Add confirmation code validation (if provided by shipper during delivery)
- Support dispute flow for incorrect deliveries


---

**Navigation**: [ Back to client Features](README.md) | [ Features Index](../README.md) | [ Report Index](../../README.md)