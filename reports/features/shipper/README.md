# Shipper Features

This document serves as an index for all shipper features in the Delivery App. Each feature is documented in a separate file with detailed activity diagrams, sequence diagrams, and implementation notes.

## Table of Contents

- [Key Surfaces](#key-surfaces)
- [Feature Index](#feature-index)
- [Upcoming Features](#upcoming-features)
- [Known Issues](#known-issues)
- [Related Documentation](#related-documentation)

## Key Surfaces

The Delivery App provides the following main user interfaces:

- **TaskFragment and TaskDetailActivity** - Assignment list and status updates
- **QrScanActivity** - Scan-to-accept flow
- **SessionDashboardFragment** - Session bootstrap (create, start, finish)
- **ChatActivity and GlobalChatService** - Real-time communication and proposals
- **MapFragment** - Routing and navigation preview

## Feature Index

### Parcel Operations

- **[Scan and Accept Parcel](scan-accept-parcel.md)** (v0/v1) - Scan QR code to accept parcel into delivery session
- **[Complete Task and Confirm Proof](complete-task.md)** (v1) - Mark delivery as completed with route metrics and photos
- **[Handle Postpone Proposal](handle-postpone.md)** (v1) - Respond to postpone requests from admin or client

### Session Management

- **[Manage Delivery Session](manage-session.md)** (v1) - Create, start, and finish delivery sessions

### Navigation

- **[Navigation and Routing](navigation-routing.md)** (v1) - View routes, get navigation directions, and see delivery locations on map

## Upcoming Features (v2)

- Paging and filter V2 for assignments list and history (reuse QueryPayload from ManagementSystem)
- Offline-first queue for status updates (store in Room DB inside repository)
- Unified navigation intent that opens OSRM or Google Maps with coordinates from Zone Service

## Known Issues

See [System Analysis](../../SYSTEM_ANALYSIS.md) for detailed analysis and recommendations:

1. **Missing assignmentId in postpone proposal** (Priority 1.1):
   - DeliveryApp must send both `assignmentId` and `parcelId` when responding to postpone proposals
   - Fix: Update `ChatActivity#onProposalRespond` to include `assignmentId` in `resultData`

2. **Status alignment**:
   - When shipper triggers postpone via quick action, also call `SessionClient.failTask` or `completeTask` depending on admin response to keep statuses aligned

## Related Documentation

- [Delivery App](../../1_CLIENTS/2_DELIVERY_APP.md) - App structure
- [Backend Services](../../2_BACKEND/) - Service details
- [API Documentation](../../3_APIS_AND_FUNCTIONS/README.md) - API documentation
- [System Analysis](../../SYSTEM_ANALYSIS.md) - System analysis and recommendations
