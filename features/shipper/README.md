## Shipper Feature Book

DeliveryApp (`DeliveryApp/app/src/main/java/com/ds/deliveryapp`) drives these journeys. Pair each activity with the fragments/activities listed below to keep implementation and documentation synced.

### Key surfaces

- `TaskFragment` + `TaskDetailActivity` – assignment list + status updates.
- `QrScanActivity` – scan-to-accept flow.
- `SessionDashboardFragment` – session bootstrap (create/start/finish).
- `ChatActivity` + `GlobalChatService` – realtime comms + proposals.
- `MapFragment` – routing and navigation preview.

### Activity – Scan & accept parcel (v0/v1)

```mermaid
flowchart TD
    A[Driver taps Scan in TaskFragment] --> B[QrScanActivity opens camera]
    B --> C{QR decoded?}
    C -- No --> B
    C -- Yes --> D[Send POST /sessions/drivers/{id}/accept-parcel]
    D --> E[Session Service returns assignmentId]
    E --> F[ParcelService updates parcel=ON_ROUTE]
    F --> G[TaskFragment refreshes assignments list]
    G --> H[MapFragment fetches route from Zone Service (optional)]
```

### Sequence – Scan & accept parcel (v0/v1)

```mermaid
sequenceDiagram
    participant Shipper as Shipper (QrScanActivity)
    participant Gateway as API Gateway
    participant SessionSvc as Session Service
    participant ParcelSvc as Parcel Service
    participant Kafka as Kafka
    participant Client as Client (WebSocket)
    
    Shipper->>Shipper: Scan QR code (parcel code)
    Shipper->>Gateway: POST /api/v1/sessions/drivers/{id}/accept-parcel<br/>{parcelCode: "ABC123"}
    Gateway->>Gateway: Extract X-User-Id from JWT
    Gateway->>SessionSvc: POST /api/v1/sessions/drivers/{id}/accept-parcel<br/>Headers: X-User-Id
    
    SessionSvc->>ParcelSvc: GET /api/v1/parcels/code/{code}
    ParcelSvc-->>SessionSvc: Parcel (id, status, receiverId)
    
    alt Active session exists
        SessionSvc->>SessionSvc: Add assignment to existing session
    else No active session
        SessionSvc->>SessionSvc: Create new session (CREATED)<br/>Add assignment
    end
    
    SessionSvc->>SessionSvc: Create DeliveryAssignment:<br/>parcelId, sessionId, deliveryManId<br/>status = ASSIGNED
    
    SessionSvc->>Kafka: Publish AssignmentCreatedEvent
    Kafka->>ParcelSvc: AssignmentCreatedEvent
    ParcelSvc->>ParcelSvc: Update parcel:<br/>status = ON_ROUTE
    
    ParcelSvc->>Kafka: Publish ParcelStatusChangedEvent
    Kafka->>Client: WebSocket notification
    Client->>Client: Refresh parcel list
    
    SessionSvc-->>Gateway: DeliveryAssignment (assignmentId, sessionId)
    Gateway-->>Shipper: Success response
    
    Shipper->>Shipper: TaskFragment refreshes<br/>MapFragment loads route (optional)
```

**Code anchors**

- API: `SessionClient.acceptParcelToSession`.
- VM: `TaskFragment` should call `SessionDashboardFragment#createSession` if no active session exists.

### Activity – Handle postpone proposal (v1)

```mermaid
flowchart TD
    A[ProposalPopupDialog] --> B[Driver chooses Accept]
    B --> C[ChatActivity.onProposalRespond]
    C --> D[POST /proposals/{id}/respond {resultData, assignmentId, parcelId}]
    D --> E[Communication Service pushes decision]
    E --> F[Session Service updates assignment=SUCCESS/POSTPONED]
    F --> G[Parcel Service sets status=DELAYED]
    G --> H[Management System receives notification]
```

**Required fixes**

- Include both `assignmentId` and `parcelId` in `resultData` payload before calling `respondToProposal`.
- When shipper triggers postpone via quick action, also call `SessionClient.failTask/completeTask` depending on admin response to keep statuses aligned.

### Activity – Complete task & confirm proof (v1)

```mermaid
flowchart TD
    A[TaskDetailActivity] --> B[Driver taps 'Giao thành công']
    B --> C[Collect route metrics + photos]
    C --> D[POST /assignments/drivers/{id}/parcels/{parcelId}/complete]
    D --> E[Session Service sets assignment=COMPLETED]
    E --> F[Parcel Service sets parcel=SUCCEEDED]
    F --> G[Communication Service notifies client]
```

### Sequence – Complete task & confirm proof (v1)

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

**Code references**

- API: `SessionClient.completeTask`.
- Frontend: `DeliveryApp/app/src/main/java/com/ds/deliveryapp/TaskDetailActivity.java`.
- Backend: `BE/session-service/src/main/java/com/ds/session/session_service/application/controllers/DeliveryAssignmentController.java`.

### Upcoming (v2)

- Paging/filter V2 for assignments list and history (reuse `QueryPayload` from ManagementSystem).
- Offline-first queue for status updates (store in Room DB inside `repository/`).
- Unified navigation intent that opens OSRM/Google Maps with coordinates from Zone Service.
