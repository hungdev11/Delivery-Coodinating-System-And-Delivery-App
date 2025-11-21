## Admin Feature Book

This page captures the management console capabilities that live in `ManagementSystem/`. Each activity flow lists the UI entry point, backend services touched, and pending improvements.

### Core responsibilities

1. **Account & role administration** – module `src/modules/Users`, backed by `User_service`.
2. **Delivery oversight** – module `src/modules/Delivery`, consuming `Session Service`, `Parcel Service`, `Communication Service`.
3. **Zone & routing configuration** – module `src/modules/Zones`, integrated with `zone_service`.
4. **System settings** – module `src/modules/Settings`, reading/writing `Settings_service`.
5. **Operations confirmation** – confirm deliveries, delays, disputes after shipper updates.

### Activity – Confirm delivery completion (v1)

```mermaid
flowchart TD
    A[Parcel list in ManagementSystem] -->|Select parcel| B[Open parcel detail drawer]
    B --> C{Parcel status == SUCCEEDED?}
    C -- No --> D[Wait for shipper update via Communication Service]
    D -->|Receive event| C
    C -- Yes --> E[Admin clicks Confirm Receipt]
    E --> F[Call API Gateway /v1/parcels/{id}/confirm-delivery]
    F --> G[Parcel Service sets parcel=DELIVERED]
    G --> H[Session Service updates assignment=SUCCESS]
    H --> I[Communication Service broadcasts notification]
    I --> J[ManagementSystem refreshes list + audit log]
```

### Sequence – Admin confirm delivery (v1)

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

**Implementation notes**

- UI hook: add action button to `ParcelsView.vue` (admin scope) and to chat sidebar when conversing with the shipper (proposal confirmation).
- Backend: expose `PUT /v1/parcels/{id}/confirm-delivery` in Parcel Service + propagate assignment update through Session Service.
- Audit: reuse `AUDIT_LOGGING_GUIDE.md` patterns (BE).
- Uses `AssignmentSnapshot` for efficient cross-service communication (same pattern as client confirmation).

### Activity – Approve postpone request (v1)

```mermaid
flowchart TD
    A[Interactive proposal popup] --> B[Admin reviews task metadata]
    B --> C{Contains assignmentId + parcelId?}
    C -- No --> D[Request shipper to resend proposal]
    C -- Yes --> E[Admin selects Accept / Decline]
    E --> F[Call Session Service /assignments/{assignmentId}/postpone]
    F --> G[Parcel Service sets status=DELAYED]
    G --> H[Communication Service sends confirmation message]
    H --> I[ManagementSystem updates Delivery Session detail]
```

### Sequence – Approve postpone request (v1)

```mermaid
sequenceDiagram
    participant Admin as Admin (ChatView)
    participant CommSvc as Communication Service
    participant SessionSvc as Session Service
    participant ParcelSvc as Parcel Service
    participant Kafka as Kafka
    participant Shipper as Shipper (WebSocket)
    
    Note over Admin,Shipper: Shipper sends POSTPONE_REQUEST proposal<br/>via ChatActivity
    
    Admin->>CommSvc: POST /proposals/{id}/respond<br/>{resultData: "ACCEPTED", assignmentId, parcelId}
    CommSvc->>CommSvc: Validate proposal exists<br/>Validate admin role
    
    CommSvc->>SessionSvc: PUT /assignments/{assignmentId}/postpone<br/>{reason, approvedBy: adminId}
    SessionSvc->>SessionSvc: Update assignment:<br/>status = POSTPONED<br/>approvedBy = adminId
    
    SessionSvc->>Kafka: Publish AssignmentUpdatedEvent
    Kafka->>ParcelSvc: AssignmentUpdatedEvent
    ParcelSvc->>ParcelSvc: Update parcel:<br/>status = DELAYED<br/>delayedUntil = newDate
    
    ParcelSvc->>Kafka: Publish ParcelStatusChangedEvent
    Kafka->>Shipper: WebSocket notification
    Kafka->>Admin: WebSocket notification
    
    CommSvc->>CommSvc: Update proposal status = ACCEPTED
    CommSvc->>Shipper: WebSocket: Proposal update
    CommSvc-->>Admin: Success response
    
    Note over Admin,Shipper: Both parties see updated status<br/>Parcel can be rescheduled
```

**Fixes required**

- DeliveryApp must send both `assignmentId` + `parcelId` when responding to postpone proposals (see `ChatActivity#onProposalRespond`).
- Session Service should expose idempotent postpone endpoint tied to assignment.

### Module checkpoints

| Module | Status | Gaps |
| --- | --- | --- |
| Users | CRUD complete | Missing V2 filter wiring on UI (API ready in BE) |
| Parcels | Basic CRUD | No admin confirmation action, no tie-in with chat events |
| Delivery | Session list + detail | Need dual session APIs (active vs history) and shipper-client filter |
| Communication | Chat UI with websocket | No inline parcel confirmation controls |
| Settings | CRUD skeleton | Missing validation + secrets guard |

### TODOs for admins

- Add dashboard widgets that combine `Session Service` KPIs + Kafka consumer lag.
- Include “current shipper vs client parcel list” fix: extend `/v1/parcels/client-view` with `shipperId` filters to support dispute resolution.
- Document monitoring hooks in `reports/2_BACKEND` once Kafka tracing is wired.
