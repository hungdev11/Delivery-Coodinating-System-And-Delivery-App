**Navigation**: [← Back to API Documentation](../api_documentation.md) | [↑ APIs and Functions](../../README.md) | [↑ Report Index](../../../README.md)

---

# Parcel Service API Documentation

The Parcel Service provides endpoints for managing the complete parcel lifecycle from creation through delivery confirmation.

## Table of Contents

- [Version 0](#version-0)
- [Version 1](#version-1)
- [Version 2](#version-2)
- [Key Features](#key-features)
- [Related Documentation](#related-documentation)

## Version 0

- **[Parcel Controller V0](v0/PARCEL_SERVICE_V0_PARCEL_CONTROLLER.md)** - Basic parcel operations

## Version 1

- **[Parcel Controller V1](v1/PARCEL_SERVICE_V1_PARCEL_CONTROLLER.md)** - Complete parcel lifecycle management including:
  - CRUD operations
  - Status management (deliver, confirm, refuse, postpone, accident)
  - Dispute management
  - Priority and delay operations
- **[Client Parcel Controller V1](v1/PARCEL_SERVICE_V1_CLIENT_PARCEL_CONTROLLER.md)** - Client-scoped parcel endpoints for receivers

## Version 2

- **[Parcel Controller V2](v2/PARCEL_SERVICE_V2_PARCEL_CONTROLLER.md)** - Advanced parcel queries with complex filtering

## Key Features

- Parcel creation with sender and receiver information
- Status tracking through delivery lifecycle
- Confirmation workflows for clients and administrators
- Dispute handling and resolution
- Advanced filtering and querying capabilities (v2)

## Related Documentation

- [Parcel Service Architecture](../../../2_BACKEND/3_PARCEL_SERVICE.md) - Service architecture and components
- [Parcel Workflows](../../diagrams/parcel_workflows.md) - Complete parcel operation workflows
- [API Gateway Parcel Endpoints](../api-gateway/v1/V1_PARCEL_SERVICE_CONTROLLER.md) - Gateway proxy endpoints

---

**Navigation**: [← Back to API Documentation](../api_documentation.md) | [↑ APIs and Functions](../../README.md) | [↑ Report Index](../../../README.md)
