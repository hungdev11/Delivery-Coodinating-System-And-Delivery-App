# Client Features

This document serves as an index for all client features in the Management System. Client personas use the same Management System frontend but with CLIENT roles. Each feature is documented in a separate file with detailed activity diagrams, sequence diagrams, and implementation notes.

## Table of Contents

- [Feature Index](#feature-index)
- [Primary Flows](#primary-flows)
- [Known Issues](#known-issues)
- [Backlog](#backlog)
- [Related Documentation](#related-documentation)

## Feature Index

### Parcel Management

- **[Create and Assign Parcel](create-parcel.md)** (v1) - Create new parcel with sender and receiver addresses
- **[Track Parcels](track-parcels.md)** (v1) - View and track all received parcels
- **[Confirm Delivery and Dispute](confirm-delivery.md)** (v1) - Confirm receipt of parcel after shipper delivery

### Address Management

- **[Manage Addresses](manage-addresses.md)** (v1) - Create, edit, and manage sender and receiver addresses

### Communication

- **[Chat and Proposals](chat-proposals.md)** (v1) - Real-time communication with shippers and admins, interactive proposals

## Primary Flows

1. **Create parcel** - `CreateParcelView.vue` posts to Parcel Service via API Gateway
2. **Manage addresses** - `MyAddressesView.vue` writes to User Service Addresses
3. **Track parcels** - `MyParcelsView.vue` consumes `/api/v1/client/parcels/received` (client-scoped API)
4. **Chat and proposals** - `Communication/ChatView.vue` for live updates and confirmations
5. **Confirm receipt** - `MyParcelsView.vue` allows clients to confirm parcel receipt after shipper delivery

## Known Issues

See [System Analysis](../../SYSTEM_ANALYSIS.md) for detailed analysis and recommendations:

1. **Missing shipperId filter**: Cannot filter parcels by `shipperId` and `receiverId` to see parcels delivered by a specific shipper
   - Fix: Extend Parcel Service search to accept `shipperId`, update API client, and add filter chips

2. **Session API limitations**: Missing exclude current parcel parameter when fetching sessions
   - Fix: Add `excludeParcelId` query parameter to `GET /v1/sessions/drivers/{id}/active`

## Backlog

- Add filter chips (delivered, on-route, delayed) with V2 filter syntax
- Provide exclude current parcel parameter when fetching sessions (bug mentioned in report)
- Support quick actions from chat to open ParcelDetail drawer (link currentParcel state to router)
- Implement Kafka consumer for AssignmentSnapshot updates (event-driven sync from Session Service)
- Add confirmation code validation (if provided by shipper during delivery)

## Related Documentation

- [System Analysis](../../SYSTEM_ANALYSIS.md) - System analysis
- [Backend Services](../../2_BACKEND/) - Service details
- [API Documentation](../../3_APIS_AND_FUNCTIONS/README.md) - API documentation
- [Management System](../../1_CLIENTS/1_MANAGEMENT_SYSTEM.md) - Web application architecture
