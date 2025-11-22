# Admin Features

This document serves as an index for all administrator features in the management console. Each feature is documented in a separate file with detailed activity diagrams, sequence diagrams, and implementation notes.

## Table of Contents

- [Core Responsibilities](#core-responsibilities)
- [Feature Index](#feature-index)
- [Module Status](#module-status)
- [Known Issues](#known-issues)
- [Related Documentation](#related-documentation)

## Core Responsibilities

Administrators have access to comprehensive system management capabilities:

1. **Account and Role Administration** - Module `src/modules/Users`, backed by User Service
2. **Delivery Oversight** - Module `src/modules/Delivery`, consuming Session Service, Parcel Service, and Communication Service
3. **Zone and Routing Configuration** - Module `src/modules/Zones`, integrated with Zone Service
4. **System Settings** - Module `src/modules/Settings`, reading and writing Settings Service
5. **Operations Confirmation** - Confirm deliveries, delays, and disputes after shipper updates
6. **Parcel Management** - Module `src/modules/Parcels`, full CRUD and status tracking via Parcel Service
7. **Communication and Proposals** - Module `src/modules/Communication`, real-time chat and interactive proposals via Communication Service

## Feature Index

### Delivery Operations

- **[Confirm Delivery Completion](confirm-delivery.md)** (v1) - Confirm delivery after shipper completes task
- **[Approve Postpone Request](approve-postpone.md)** (v1) - Review and approve or decline postpone requests from shippers

### Parcel Management

- **[Create and Manage Parcels](manage-parcels.md)** (v1) - Full CRUD operations for parcels

### Session Management

- **[Manage Delivery Sessions](manage-sessions.md)** (v1) - View, create, and manage delivery sessions

### System Configuration

- **[Manage Zones and Routing](manage-zones.md)** (v1) - Configure delivery zones and routing
- **[User and Role Management](manage-users.md)** (v1) - Create and manage users and roles

## Module Status

| Module | Status | Gaps |
| --- | --- | --- |
| Users | CRUD complete | Missing V2 filter wiring on UI (API ready in backend) |
| Parcels | Basic CRUD | No admin confirmation action, no tie-in with chat events |
| Delivery | Session list and detail | Need dual session APIs (active vs history) and shipper-client filter |
| Communication | Chat UI with WebSocket | No inline parcel confirmation controls |
| Settings | CRUD skeleton | Missing validation and secrets guard |
| Zones | CRUD and routing | OSRM integration complete, needs monitoring |

## Known Issues

See [System Analysis](../../SYSTEM_ANALYSIS.md) for detailed analysis and recommendations:

1. **Parcel list filter missing shipperId**: Client cannot filter parcels by `shipperId` and `receiverId`
   - Fix: Extend Parcel Service search to accept `shipperId`, update API client, and add filter chips

2. **Session API limitations**: Only active session returned; cannot request all sessions or exclude current parcel
   - Fix: Add `excludeParcelId` query parameter to `GET /v1/sessions/drivers/{id}/active` and provide second endpoint for history

3. **Admin confirmation missing**: After shipper completes task there is no admin confirmation action
   - Fix: Add confirm action to parcel detail and chat quick actions; expose `/parcels/{id}/confirm-delivery`

## Related Documentation

- [System Analysis](../../SYSTEM_ANALYSIS.md) - System analysis and identified issues
- [Backend Services](../../2_BACKEND/) - Service details
- [API Documentation](../../3_APIS_AND_FUNCTIONS/README.md) - API documentation
- [Management System](../../1_CLIENTS/1_MANAGEMENT_SYSTEM.md) - Web application architecture
