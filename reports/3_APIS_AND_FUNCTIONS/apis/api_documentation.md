**Navigation**: [← Back to APIs and Functions](../README.md) | [↑ Report Index](../../README.md)

---

# API Documentation Overview

This document provides an overview of the API endpoints available across all microservices in the Delivery System. Detailed endpoint documentation is organized by service and API version.

## Table of Contents

- [API Versioning](#api-versioning)
- [Service Organization](#service-organization)
- [System Diagrams](#system-diagrams)
- [Authentication](#authentication)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)
- [API Gateway Access](#api-gateway-access)

## API Versioning

The system uses versioned APIs to support backward compatibility and gradual feature introduction:

- **Version 0 (v0)**: Basic functionality, initial implementation
- **Version 1 (v1)**: Enhanced features with improved functionality
- **Version 2 (v2)**: Advanced features including complex filtering and querying capabilities

## Service Organization

API endpoints are organized by service:

### User Service

The User Service provides endpoints for user management, delivery personnel profiles, and address management. Endpoints support creating users, managing addresses, and querying user information.

**Key Endpoints:**
- User CRUD operations: `/api/v1/users`
- Address management: `/api/v1/users/{userId}/addresses`
- Delivery personnel: `/api/v1/users/{userId}/delivery-man`
- Advanced queries: `/api/v2/users` (with filtering support)

**Detailed Documentation:**
- See [User Service API Documentation](apis/user-service/README.md) for complete endpoint reference

### Settings Service

The Settings Service provides centralized configuration management with support for grouped settings and bulk operations.

**Key Endpoints:**
- Setting management: `/api/v1/settings/{group}/{key}`
- Bulk operations: `/api/v1/settings/{group}/bulk`
- Health checks: `/health`

**Detailed Documentation:**
- See [Settings Service API Documentation](apis/settings-service/README.md) for complete endpoint reference

### Session Service

The Session Service manages delivery sessions and parcel assignments. It handles session creation, assignment tracking, and status updates.

**Key Endpoints:**
- Session management: `/api/v1/sessions`
- Parcel acceptance: `/api/v1/sessions/drivers/{id}/accept-parcel`
- Assignment operations: `/api/v1/assignments`
- QR code generation: `/api/v1/qr`

**Detailed Documentation:**
- See [Session Service API Documentation](apis/session-service/README.md) for complete endpoint reference

### Parcel Service

The Parcel Service manages parcel lifecycle from creation through delivery confirmation. It coordinates with other services for customer and routing information.

**Key Endpoints:**
- Parcel CRUD: `/api/v1/parcels`
- Client-specific: `/api/v1/client/parcels` (receiver-scoped endpoints)
  - Get received parcels: `POST /api/v1/client/parcels/received`
  - Confirm parcel: `POST /api/v1/client/parcels/{id}/confirm`
- Confirmation: `/api/v1/parcels/{id}/confirm-delivery`
- Advanced queries: `/api/v2/parcels` (with filtering support)

**Detailed Documentation:**
- See [Parcel Service API Documentation](apis/parcel-service/README.md) for complete endpoint reference

### Communication Service

The Communication Service provides real-time messaging and interactive proposals through WebSocket and REST APIs.

**Key Endpoints:**
- Conversations: `/api/v1/conversations`
- Messages: `/api/v1/conversations/{id}/messages`
- Proposals: `/api/v1/proposals`
- Proposal Configurations: `/api/v1/admin/proposals/configs` (admin only)
- WebSocket Chat: `/ws` (STOMP endpoints for real-time messaging)
- Notifications: `/api/v1/notifications`

**Detailed Documentation:**
- See [Communication Service API Documentation](apis/communication-service/README.md) for complete endpoint reference

### Zone Service

The Zone Service handles geographic data, zones, and route calculation. It integrates with OSRM for route optimization.

**Key Endpoints:**
- Zone management: `/api/v1/zones`
- Address operations: `/api/v1/addresses`
- Route calculation: `/api/v1/routing/calculate`
- OSRM data: `/api/v1/osrm-data`

**Detailed Documentation:**
- See [Zone Service API Documentation](apis/zone-service/README.md) for complete endpoint reference

### API Gateway

The API Gateway provides proxy endpoints that route requests to appropriate backend services. Gateway endpoints mirror backend service endpoints with version prefixes.

**Key Endpoint Groups:**
- User operations: `/api/v0/users`, `/api/v1/users`, `/api/v2/users`
- Parcel operations: `/api/v0/parcels`, `/api/v1/parcels`, `/api/v2/parcels`
- Client parcel operations: `/api/v1/client/parcels` (client-scoped endpoints)
- Session operations: `/api/v1/sessions`, `/api/v2/delivery-sessions`
- Communication: `/api/v1/conversations`, `/api/v1/proposals`, `/api/v1/admin/proposals/configs`
- Zone operations: `/api/v1/zones`, `/api/v1/routing`
- Settings: `/api/v0/settings`, `/api/v1/settings`, `/api/v2/settings`

**Detailed Documentation:**
- See [API Gateway API Documentation](apis/api-gateway/README.md) for complete endpoint reference organized by version

## System Diagrams

System workflows and entity lifecycles are documented in the [diagrams](diagrams/) folder:

- **[Parcel Workflows](diagrams/parcel_workflows.md)** - Complete parcel operation workflows including creation, acceptance, delivery, confirmation, refusal, and dispute handling
- **[Session Workflows](diagrams/session_workflows.md)** - Delivery session workflows including preparation, starting, completion, and failure handling
- **[Routing Workflows](diagrams/routing_workflows.md)** - Route calculation and OSRM integration workflows
- **[System Architecture Flows](diagrams/system_flows.md)** - Authentication and system-level data flow patterns

## Authentication

All API endpoints require authentication through Keycloak. The API Gateway validates JSON Web Tokens (JWT) and forwards user identity information to backend services via headers.

## Error Handling

API endpoints return standard HTTP status codes:
- 200 OK: Successful operation
- 201 Created: Resource created successfully
- 400 Bad Request: Invalid request data
- 401 Unauthorized: Authentication required
- 403 Forbidden: Insufficient permissions
- 404 Not Found: Resource not found
- 500 Internal Server Error: Server-side error

Error responses include detailed error messages to help diagnose issues.

## Rate Limiting

API endpoints may be subject to rate limiting to ensure system stability. Clients should implement appropriate retry logic with exponential backoff.

## API Gateway Access

All client applications should access backend services through the API Gateway at port 21500. Direct access to individual microservices is not recommended for production use.

## Related Documentation

- [APIs and Functions README](../README.md) - Complete API documentation index
- [Backend Services](../../2_BACKEND/) - Service architecture documentation
- [Features Documentation](../../features/README.md) - Feature workflows with API references

---

**Navigation**: [← Back to APIs and Functions](../README.md) | [↑ Report Index](../../README.md)
