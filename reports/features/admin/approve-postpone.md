**Navigation**: [ Back to admin Features](README.md) | [ Features Index](../README.md) | [ Report Index](../../README.md)

---

# Admin: Approve Postpone Request

**Version**: v1  
**Module**: `ManagementSystem/src/modules/Communication`  
**Related**: See `../../SYSTEM_ANALYSIS.md` for system analysis and recommendations

## Overview

Admin reviews and approves/declines postpone requests from shippers via interactive proposals in chat. This flow updates assignment status to `POSTPONED` and marks parcel as `DELAYED`.

## Activity Diagram

```mermaid
flowchart TD
    A["Interactive proposal popup"] --> B["Admin reviews task metadata"]
    B --> C{"Contains assignmentId + parcelId?"}
    C -- "No" --> D["Request shipper to resend proposal"]
    C -- "Yes" --> E["Admin selects Accept / Decline"]
    E --> F["Call Session Service PUT /assignments/{assignmentId}/postpone"]
    F --> G["Parcel Service sets status=DELAYED"]
    G --> H["Communication Service sends confirmation message"]
    H --> I["ManagementSystem updates Delivery Session detail"]
```

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Admin as Admin (ChatView)
    participant CommSvc as Communication Service
    participant SessionSvc as Session Service
    participant ParcelSvc as Parcel Service
    participant Kafka as Kafka
    participant Shipper as Shipper (WebSocket)
    
    Note over Admin,Shipper: Shipper sends POSTPONE_REQUEST proposal<br/>via ChatActivity
    
    activate Admin
    Admin->>CommSvc: POST /proposals/{id}/respond<br/>{resultData: "ACCEPTED", assignmentId, parcelId}
    activate CommSvc
    CommSvc->>CommSvc: Validate proposal exists<br/>Validate admin role
    
    CommSvc->>SessionSvc: PUT /assignments/{assignmentId}/postpone<br/>{reason, approvedBy: adminId}
    activate SessionSvc
    SessionSvc->>SessionSvc: Update assignment:<br/>status = POSTPONED<br/>approvedBy = adminId
    SessionSvc->>Kafka: Publish AssignmentUpdatedEvent
    activate Kafka
    Kafka->>ParcelSvc: AssignmentUpdatedEvent
    activate ParcelSvc
    ParcelSvc->>ParcelSvc: Update parcel:<br/>status = DELAYED<br/>delayedUntil = newDate
    ParcelSvc->>Kafka: Publish ParcelStatusChangedEvent
    deactivate ParcelSvc
    Kafka->>Shipper: WebSocket notification
    activate Shipper
    deactivate Shipper
    Kafka->>Admin: WebSocket notification
    deactivate Kafka
    deactivate SessionSvc
    CommSvc->>CommSvc: Update proposal status = ACCEPTED
    CommSvc->>Shipper: WebSocket: Proposal update
    activate Shipper
    deactivate Shipper
    deactivate CommSvc
    CommSvc-->>Admin: Success response
    deactivate Admin
    
    Note over Admin,Shipper: Both parties see updated status<br/>Parcel can be rescheduled
```

## Implementation Notes

- **UI**: Interactive proposal popup in `ChatView.vue` when shipper sends `POSTPONE_REQUEST`.
- **Validation**: Proposal must contain both `assignmentId` and `parcelId` in `resultData`.
- **Backend**: Session Service should expose idempotent postpone endpoint tied to assignment.

## API References

- **Communication Service**: `POST /api/v1/proposals/{id}/respond` (see [Communication Service V1 PROPOSAL_CONTROLLER Controller](../../3_APIS_AND_FUNCTIONS/apis/communication-service/v1/COMMUNICATION_SERVICE_V1_PROPOSAL_CONTROLLER.md))
- **Session Service**: `PUT /api/v1/assignments/{assignmentId}/postpone` (to be implemented, see `../../SYSTEM_ANALYSIS.md`)

## Code References

- **Frontend**: `ManagementSystem/src/modules/Communication/ChatView.vue`
- **Backend Communication**: `BE/communication_service/src/main/java/com/ds/communication_service/application/controllers/ProposalController.java`
- **Backend Session**: `BE/session-service/src/main/java/com/ds/session/session_service/application/controllers/DeliveryAssignmentController.java`

## Known Issues & Fixes Required

1. **Missing assignmentId in proposal response** (see `../../SYSTEM_ANALYSIS.md` section 3):
   - DeliveryApp must send both `assignmentId` + `parcelId` when responding to postpone proposals
   - Fix: Update `ChatActivity#onProposalRespond` to include `assignmentId` in `resultData`

2. **Missing postpone endpoint**:
   - Session Service should expose `PUT /api/v1/assignments/{assignmentId}/postpone`
   - Fix: Implement endpoint in `DeliveryAssignmentController` (see `../../SYSTEM_ANALYSIS.md` section 4)


---

**Navigation**: [ Back to admin Features](README.md) | [ Features Index](../README.md) | [ Report Index](../../README.md)
