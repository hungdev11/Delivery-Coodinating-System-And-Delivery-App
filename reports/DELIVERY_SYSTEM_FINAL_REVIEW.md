## Delivery Platform Final Review

### 1. Architecture Snapshot

- **Entry point**: API Gateway (`BE/api-gateway`, port 21500) authenticates via Keycloak and forwards to downstream services (`BE/README.md`).
- **Microservices**:
  - `User_service` (21501): user, shipper identity, Keycloak sync.
  - `Settings_service` (21502): centralized configuration.
  - `zone_service` (21503): spatial data, routing, OSRM integration.
  - `parcel-service`: parcel lifecycle, integrates with User + Zone + Session.
  - `session-service`: sessions + assignments, emits events to Communication.
  - `communication_service`: chat, websocket gateway, proposals, notifications.
- **Shared components**: Kafka broker (event fan-out), Setting Service for secrets, OSRM external routing, Keycloak IAM. High-level diagrams already live in `/reports/overall_package_uml.md` and should be regenerated after the updates below.
- **Clients**: ManagementSystem (Vue + Nuxt UI) for admin/client personas, DeliveryApp (Android) for shippers. Both consume Gateway routes and subscribe to Communication WS.

### 2. Order / Session / Task Flow

#### v0 – Minimal scan-and-go
1. Shipper scans parcel QR (`DeliveryApp/QrScanActivity`), app hits `POST /sessions/drivers/{id}/accept-parcel`.
2. Session Service creates (or reuses) session, adds assignment, returns metadata (see `BE/session-service/.docs/route/sessions.md`).
3. Parcel Service marks parcel `ON_ROUTE`, ManagementSystem only observes via parcel list.

#### v1 – Full collaboration (current target)
1. **Order intake**: Admin/client create parcel via ManagementSystem (`ParcelsView`, `Client/CreateParcelView`). Parcel Service persists destinations, Zone Service resolves routing cells.
2. **Session planning**:
   - Admin pushes tasks via Session Service (`POST /v1/sessions` or assignments importer) or
   - Shipper self-creates session from DeliveryApp dashboard (`SessionDashboardFragment` → `SessionClient.createSessionPrepared/startSession`).
3. **Execution**:
   - Task list lives in Session Service assignments table.
   - DeliveryApp updates status via `SessionClient.completeTask/failTask/refuseTask`.
   - Route info pulled from Zone Service via `RoutingApi`.
4. **Realtime coordination**:
   - Communication Service proxies chat + interactive proposals.
   - Quick actions (postpone, confirm refusal) must carry `assignmentId` and `parcelId` to keep Parcel + Session in sync.
5. **Closure**:
   - Admin/client confirm final state (new feature) via Parcel Service endpoint → Session assignment set to `SUCCESS`, Parcel to `DELIVERED`.
   - Kafka broadcast updates UI dashboards.

#### v2 – Enhanced querying/pagination (future)
1. All list endpoints expose `/api/v2/...` with alternating filter/operator structure (`BE/FILTER_SYSTEM_V0_V1_V2_GUIDE.md`, `BE/QUERY_SYSTEM.md`).
2. ManagementSystem uses QueryPayload V2 (already available in `src/common/types/filter.ts`).
3. DeliveryApp assignments/history should adopt the same payload for consistency and to support offline caching.

### 3. System Checks

#### Backend
- **Session Service**: supports accept/start/complete/fail but lacks explicit postpone endpoint tied to assignment ID. Introduce `PUT /v1/assignments/{assignmentId}/postpone` and emit Kafka event for Parcel Service.
- **Parcel Service**: needs admin/client confirmation route (after shipper success) and ability to tag parcel with `DELIVERED` vs `DELAYED`.
- **Communication Service**: acts as websocket gateway plus HTTP proposals. Confirm that `proposal_type_configs` contain metadata for assignment/parcel; enforce schema validation so apps always send both IDs.
- **API Gateway**: add routing for new confirm/delay endpoints and double-check `@RestController` v2 proxies (Users already done per `SERVICES_UPDATE_GUIDE.md`).
- **Settings Service**: DTOs exist but controllers still todo (per `SERVICES_UPDATE_GUIDE.md`). Finish to avoid leaking secrets across services.

#### DeliveryApp
- Uses Retrofit clients for Parcels + Sessions + Communication. `ChatActivity` injects `parcelId` into `POSTPONE_REQUEST`, but **no assignmentId is attached**, so backend cannot update the right task (`DeliveryApp/app/src/main/java/com/ds/deliveryapp/ChatActivity.java` lines 1093-1108).
- `ChatWebSocketManager.sendQuickAction` blindly forwards whatever payload is provided, meaning server-side enforcement must be added (no schema).
- Session awareness lives in fragments, but there is no UI for “confirm admin acknowledgement”.

#### ManagementSystem
- `useDeliverySessions` normalizes data from `/v1/sessions/drivers/{id}/active` and `/v1/assignments/.../tasks` into a single list, but there is no API for “all sessions” vs “active only” and no ability to exclude the current parcel when a client views shipper assignments (`ManagementSystem/src/modules/Communication/ChatView.vue` lines 210-249).
- Parcel lists for clients filter only by receiverId; there is no cross-filter for `shipperId` + `clientId`, so requirement “shipper giao cho mình” fails.
- Missing UI action for confirming receipt after shipper declares success.

### 4. Bug & Gap Tracker

| Area | Issue | Root Cause / Evidence | Fix |
| --- | --- | --- | --- |
| DeliveryApp postpone confirmation | Proposal responses lack assignment ID → backend updates wrong record | `ChatActivity` only injects `parcelId` before calling `respondToProposal` | Include `assignmentId` in `resultData` (JSON) and let Communication Service call Session Service `POST /assignments/{assignmentId}/postpone`. Ensure Parcel Service marks parcel `DELAYED`. |
| ManagementSystem session fetch | Only active session returned; cannot request “all sessions” or skip current parcel | `loadActiveSessionAndAssignments` strictly hits `/sessions/drivers/{id}/active` and then loads every assignment (see snippet below) | Add `GET /v1/sessions/drivers/{id}?excludeParcelId=` API and duplicate hook in UI. Provide second endpoint for "history" to avoid client-side manual grouping. |
| ManagementSystem parcel list by shipper | UI cannot combine `receiverId` + `shipperId` filters | `MyParcelsView` filters only by receiver; chat view uses assignments but not parcel query | Extend Parcel Service search to accept `shipperId`, update API client, and add filter chips. |
| Admin confirmation missing | After shipper completes task there is no admin/client confirmation action | No UI button or backend route | Add confirm action to parcel detail + chat quick actions; expose `/parcels/{id}/confirm-delivery` & `/parcels/{id}/client-confirm`. |
| User service vs parcel management | Lack of user-level parcel controls (bulk update, linking clients) | No endpoints and UI forms | Extend `User_service` to expose client profile data used by Parcel Service (owner info) and wire to ManagementSystem forms. |

```210:249:ManagementSystem/src/modules/Communication/ChatView.vue
const loadActiveSessionAndAssignments = async () => {
  const shipperId = isClient.value ? partnerId.value : currentUserId.value
  if (!shipperId) return
  loadingSession.value = true
  try {
    const sessionResponse = await getActiveSessionForDeliveryMan(shipperId)
    if (sessionResponse.result && sessionResponse.result.id) {
      activeSessionId.value = sessionResponse.result.id
      const assignmentsResponse = await getAssignmentsBySessionId(activeSessionId.value, {
        page: 0,
        size: 100,
      })
      if (assignmentsResponse.content) {
        sessionAssignments.value = assignmentsResponse.content
      }
    } else {
      activeSessionId.value = null
      sessionAssignments.value = []
    }
  } finally {
    loadingSession.value = false
  }
}
```

```1093:1108:DeliveryApp/app/src/main/java/com/ds/deliveryapp/ChatActivity.java
if ("POSTPONE_REQUEST".equals(type) && mParcelId != null) {
    try {
        JSONObject json = new JSONObject(data);
        json.put("parcelId", mParcelId);
        data = json.toString();
    } catch (Exception e) {
        data = "{\"parcelId\":\"" + mParcelId + "\"}";
    }
}
...
Call<InteractiveProposal> call = mChatClient.respondToProposal(
        proposalId,
        mCurrentUserId,
        payload
);
```

### 5. Documentation & Implementation Updates

| File / Area | Update |
| --- | --- |
| `reports/DELIVERY_SYSTEM_FINAL_REVIEW.md` (this file) | Keep as master checklist; reference in project README. |
| `BE/README.md` | Add Parcel, Session, Communication services to architecture diagram; document new confirm/postpone APIs and Kafka topics. |
| `BE/SERVICES_UPDATE_GUIDE.md` | Mark Settings Service controllers as TODO, add Parcel/Session V2 adoption status. |
| `ManagementSystem/README.md` | Include Delivery, Client modules (currently missing), describe chat-based confirmation feature. |
| `DeliveryApp/app/src/main/...` | Document GlobalChatService & proposal handling in project README; highlight need for assignment IDs. |
| `features/**/README.md` | (New) Provide persona-specific feature specs + Mermaid activities; update as features evolve. |
| `reports/2_BACKEND/*` | Append details on Communication/Session/Parcel interactions once APIs finalized. |

### 6. UML & Tooling Plan

Recommended diagrams to regenerate:

1. **Deployment diagram** – API Gateway + microservices + external systems (Keycloak, OSRM, Kafka). Use Structurizr DSL exported to `reports/overall_package_uml_layered.md`.
2. **Order lifecycle sequence** – Admin/Client → Gateway → Parcel Service → Session Service → Communication Service (Mermaid or PlantUML).
3. **Session activity diagram** – Shipper app flow from session creation to completion (Mermaid snippet already in `features/shipper/README.md`).
4. **Management confirmation activity** – Provided in `features/admin/README.md`.
5. **Client dispute activity** – Provided in `features/client/README.md`.

Tooling reminders (see `features/README.md`):

- **Mermaid** for quick edits inside README files (`npx @mermaid-js/mermaid-cli …`).
- **PlantUML** for richer sequence diagrams (IDE plugins).
- **Structurizr Lite** for architecture modeling; keep DSL under `reports/`.

### 7. Next Steps

1. Implement backend fixes (assignment postpone endpoint, parcel confirmation API).
2. Patch DeliveryApp proposal payload + ManagementSystem session APIs.
3. Update Swagger/OpenAPI specs and regenerate Postman collections.
4. Re-render UML diagrams using new READMEs as source.
5. Add QA checklist (unit/integration + manual flows) once code changes land.

### 8. Client Confirmation Flow Design

| Step | Component | Description |
| --- | --- | --- |
| 1 | ManagementSystem (client) | Client presses “Confirm received” on parcel detail/chat sidebar. UI calls `POST /api/v1/client/parcels/{parcelId}/confirm` via Gateway with JWT context. |
| 2 | API Gateway | New controller `ClientParcelController` validates JWT via `@AuthRequired`, forwards request (with `X-User-Id`, `X-User-Roles`) to Parcel Service `/api/v1/client/parcels/{parcelId}/confirm`. |
| 3 | Parcel Service | New endpoint validates parcel status (`SUCCEEDED`/`ON_ROUTE`), ensures receiver matches `X-User-Id`, sets parcel status to `DELIVERED`, records timestamp/audit. Publishes Kafka event `parcel.confirmed`. |
| 4 | Session Service | Listens to Kafka event or direct REST call `PUT /api/v1/assignments/{assignmentId}/status` to mark linked assignment `SUCCESS`. Optionally triggers compensation if mismatch. |
| 5 | Communication Service | Emits WebSocket notification to shipper/admin so chat timeline reflects confirmation. |
| 6 | ManagementSystem UI | Optimistically updates parcel list + conversation timeline; shows toast. |

**API contracts**

- **Gateway → Parcel Service**: `POST /api/v1/client/parcels/{parcelId}/confirm`
  - Headers: `X-User-Id`, `X-User-Roles`.
  - Body: `{ "assignmentId": "uuid", "feedback": "optional string" }`.
  - Response: `200 OK` with `{ result: { parcelId, status: "DELIVERED", confirmedAt } }`.
- **Parcel Service internal**:
  - Validate ownership & status.
  - Update parcel state, persist confirmation metadata in new columns (`confirmedAt`, `confirmedBy`, `confirmationNote`).
  - Invoke Session Service: `PUT /api/v1/assignments/{assignmentId}/status` with `{ status: "SUCCESS", reason: "CLIENT_CONFIRMED" }`.
  - Emit Kafka event `parcel.confirmed`.
- **Communication notification**: `notify.confirmation` payload with parcelId, assignmentId, userId for UI.

**UI updates**

- `ManagementSystem/src/modules/Client/MyParcelsView.vue`: add action column/button for parcels in `SUCCEEDED` status.
- `Communication/ChatView.vue`: render quick action “Confirm received” inside parcel context card.
- Toast + skeleton handling reuses existing `useToast`.

**Validation & audit**

- Prevent duplicate confirmations by checking `parcel.confirmedAt`.
- Log all confirmations via existing audit logger (`AUDIT_LOGGING_GUIDE.md` pattern) including userId, parcelId, sessionId.

**Testing checklist**

1. Client confirms parcel with matching assignment → parcel `DELIVERED`, assignment `SUCCESS`.
2. Unauthorized user (different client) gets 403.
3. Repeated confirm returns idempotent success (no double events).
4. Websocket notification appears in chat.
5. Admin dashboard reflects new status without full refresh.
