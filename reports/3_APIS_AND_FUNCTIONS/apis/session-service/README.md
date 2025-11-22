**Navigation**: [← Back to API Documentation](../api_documentation.md) | [↑ APIs and Functions](../../README.md) | [↑ Report Index](../../../README.md)

---

# Session Service API Documentation

The Session Service provides endpoints for managing delivery sessions and parcel assignments.

## Table of Contents

- [Version 0](#version-0)
- [Version 1](#version-1)
- [Version 2](#version-2)
- [Key Features](#key-features)
- [Related Documentation](#related-documentation)

## Version 0

- **[Delivery Assignment Controller V0](v0/SESSION_SERVICE_V0_DELIVERY_ASSIGNMENT_CONTROLLER.md)** - Basic assignment operations

## Version 1

- **[Session Controller V1](v1/SESSION_SERVICE_V1_SESSION_CONTROLLER.md)** - Session management operations including:
  - Session creation and management
  - Parcel acceptance
  - Session status updates
- **[Delivery Assignment Controller V1](v1/SESSION_SERVICE_V1_DELIVERY_ASSIGNMENT_CONTROLLER.md)** - Assignment operations
- **[QR Controller V1](v1/SESSION_SERVICE_V1_QR_CONTROLLER.md)** - QR code generation for parcels

## Version 2

- **[Delivery Session Controller V2](v2/SESSION_SERVICE_V2_DELIVERY_SESSION_CONTROLLER.md)** - Advanced session queries with filtering
- **[Delivery Assignment Controller V2](v2/SESSION_SERVICE_V2_DELIVERY_ASSIGNMENT_CONTROLLER.md)** - Advanced assignment queries with filtering

## Key Features

- Delivery session creation and management
- Parcel assignment to sessions
- QR code generation for parcel scanning
- Session status tracking (prepared, active, completed, failed)
- Assignment status management
- Advanced filtering and querying capabilities (v2)

## Related Documentation

- [Session Service Architecture](../../../2_BACKEND/4_SESSION_SERVICE.md) - Service architecture and components
- [Session Workflows](../../diagrams/session_workflows.md) - Complete session operation workflows
- [API Gateway Session Endpoints](../api-gateway/v1/V1_SESSION_CONTROLLER.md) - Gateway proxy endpoints

---

**Navigation**: [← Back to API Documentation](../api_documentation.md) | [↑ APIs and Functions](../../README.md) | [↑ Report Index](../../../README.md)
