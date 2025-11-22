# Bug Fixes Implementation Plan

## Priority 1: Critical Bugs

### 1. Fix Proposal Postpone - Assignment ID Missing

**Problem**: When shipper accepts postpone proposal, backend cannot find the correct assignment because it only has `parcelId`, not `assignmentId`.

**Solution**:
1. Add endpoint `PUT /assignments/{assignmentId}/postpone` in Session Service
2. Update `ProposalService.callPostponeParcelApi()` to find `assignmentId` from `parcelId` + `deliveryManId`, then call new endpoint
3. Update DeliveryApp to optionally include `assignmentId` in proposal response data

**Files to modify**:
- `BE/session-service/src/main/java/com/ds/session/session_service/application/controllers/DeliveryAssignmentController.java` - Add new endpoint
- `BE/session-service/src/main/java/com/ds/session/session_service/common/interfaces/IDeliveryAssignmentService.java` - Add method signature
- `BE/session-service/src/main/java/com/ds/session/session_service/business/v1/services/DeliveryAssignmentService.java` - Implement method
- `BE/communication_service/src/main/java/com/ds/communication_service/business/v1/services/ProposalService.java` - Update `callPostponeParcelApi()`

### 2. Fix ManagementSystem Session API

**Problem**: Only `GET /v1/sessions/drivers/{id}/active` exists, cannot get all sessions or exclude current parcel.

**Solution**:
1. Session Service already has `POST /v2/delivery-sessions` with filters - use this
2. Add helper method in ManagementSystem to query all sessions for a shipper
3. Add `excludeParcelId` query param to active session endpoint (optional)

**Files to modify**:
- `BE/session-service/src/main/java/com/ds/session/session_service/application/controllers/SessionController.java` - Add `excludeParcelId` param
- `BE/session-service/src/main/java/com/ds/session/session_service/business/v1/services/SessionService.java` - Implement filter
- `ManagementSystem/src/modules/Delivery/composables/useDeliverySessions.ts` - Add method to get all sessions

### 3. Fix Parcel List Filter - Add ShipperId

**Problem**: Client cannot filter parcels by `shipperId` + `receiverId` to see "parcels delivered by this shipper to me".

**Solution**:
1. Parcel Service already supports V2 filtering - extend filter to accept `shipperId` (cross-service join)
2. Or: Add endpoint `GET /parcels/client/{clientId}/shipper/{shipperId}` in Parcel Service
3. Update ManagementSystem API client and UI

**Files to modify**:
- `BE/parcel-service/...` - Add shipperId filter support
- `ManagementSystem/src/modules/Parcels/api.ts` - Add new endpoint or extend filter
- `ManagementSystem/src/modules/Client/MyParcelsView.vue` - Add shipperId filter UI

## Priority 2: Missing Features

### 4. Add Confirm Delivery UI

**Problem**: After shipper marks delivery as successful, admin/client cannot confirm receipt.

**Solution**:
1. Backend already has `PUT /v1/parcels/{parcelId}/confirm` - use it
2. Add button in ParcelsView (admin) and ChatView (client)
3. Add quick action in chat messages

**Files to modify**:
- `ManagementSystem/src/modules/Parcels/ParcelsView.vue` - Add confirm button
- `ManagementSystem/src/modules/Communication/ChatView.vue` - Add confirm action
- `ManagementSystem/src/modules/Parcels/api.ts` - Add confirmParcel API call

## Implementation Order

1. Fix Proposal Postpone (Priority 1.1)
2. Fix Session API (Priority 1.2)
3. Add Confirm Delivery UI (Priority 2.1) - Backend ready, just need UI
4. Fix Parcel List Filter (Priority 1.3) - Most complex, needs cross-service query
