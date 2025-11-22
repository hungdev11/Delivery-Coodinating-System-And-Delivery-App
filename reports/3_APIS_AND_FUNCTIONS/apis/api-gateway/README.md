**Navigation**: [← Back to API Documentation](../api_documentation.md) | [↑ APIs and Functions](../../README.md) | [↑ Report Index](../../../README.md)

---

# API Gateway Endpoints

The API Gateway serves as the single entry point for all client requests. It handles authentication, request routing, and provides a unified interface to backend microservices.

## Table of Contents

- [Overview](#overview)
- [Version 0 Controllers](#version-0-controllers)
- [Version 1 Controllers](#version-1-controllers)
- [Version 2 Controllers](#version-2-controllers)
- [Authentication](#authentication)
- [Request Routing](#request-routing)
- [Related Documentation](#related-documentation)

## Overview

All client applications should access backend services through the API Gateway. The Gateway validates authentication tokens, extracts user identity information, and forwards requests to appropriate backend services with necessary headers.

## Version 0 Controllers

Basic proxy endpoints providing fundamental functionality:

- **[Parcel Service Controller](v0/V0_PARCEL_SERVICE_CONTROLLER.md)** - Basic parcel operations
- **[Delivery Assignment Controller](v0/V0_DELIVERY_ASSIGNMENT_CONTROLLER.md)** - Basic assignment operations
- **[User Controller](v0/V0_USER_CONTROLLER.md)** - Basic user operations
- **[Settings Proxy Controller](v0/V0_SETTINGS_PROXY_CONTROLLER.md)** - Basic settings operations
- **[Zone Proxy Controller](v0/V0_ZONE_PROXY_CONTROLLER.md)** - Basic zone operations

## Version 1 Controllers

Enhanced proxy endpoints with improved functionality:

- **[Parcel Service Controller](v1/V1_PARCEL_SERVICE_CONTROLLER.md)** - Enhanced parcel operations
- **[Delivery Session Controller](v1/V1_DELIVERY_SESSION_CONTROLLER.md)** - Session management
- **[Delivery Assignment Controller](v1/V1_DELIVERY_ASSIGNMENT_CONTROLLER.md)** - Assignment operations
- **[Session Controller](v1/V1_SESSION_CONTROLLER.md)** - Session coordination
- **[QR Controller](v1/V1_QR_CONTROLLER.md)** - QR code operations
- **[User Controller](v1/V1_USER_CONTROLLER.md)** - User management
- **[User Address Proxy Controller](v1/V1_USER_ADDRESS_PROXY_CONTROLLER.md)** - Address management
- **[Zone Proxy Controller](v1/V1_ZONE_PROXY_CONTROLLER.md)** - Zone and routing operations
- **[Settings Proxy Controller](v1/V1_SETTINGS_PROXY_CONTROLLER.md)** - Settings management
- **[Communication Controller](v1/V1_COMMUNICATION_CONTROLLER.md)** - Real-time messaging
- **[Client Delivery Session Controller](v1/V1_CLIENT_DELIVERY_SESSION_CONTROLLER.md)** - Client-specific session operations
- **[Client Parcel Controller](v1/V1_CLIENT_PARCEL_CONTROLLER.md)** - Client-scoped parcel operations
- **[Auth Controller](v1/V1_AUTH_CONTROLLER.md)** - Authentication operations
- **[Health Controller](v1/V1_HEALTH_CONTROLLER.md)** - Health check endpoints
- **[Parcel Seed Proxy Controller](v1/V1_PARCEL_SEED_PROXY_CONTROLLER.md)** - Parcel seeding operations

## Version 2 Controllers

Advanced proxy endpoints with complex filtering and querying:

- **[Parcel Service Controller](v2/V2_PARCEL_SERVICE_CONTROLLER.md)** - Advanced parcel queries
- **[Delivery Session Controller](v2/V2_DELIVERY_SESSION_CONTROLLER.md)** - Advanced session queries
- **[Delivery Assignment Controller](v2/V2_DELIVERY_ASSIGNMENT_CONTROLLER.md)** - Advanced assignment queries
- **[User Controller](v2/V2_USER_CONTROLLER.md)** - Advanced user queries
- **[User Shipper Controller](v2/V2_USER_SHIPPER_CONTROLLER.md)** - Delivery personnel queries
- **[Settings Proxy Controller](v2/V2_SETTINGS_PROXY_CONTROLLER.md)** - Advanced settings operations
- **[Zone Proxy Controller](v2/V2_ZONE_PROXY_CONTROLLER.md)** - Advanced zone operations
- **[Client Delivery Session Controller](v2/V2_CLIENT_DELIVERY_SESSION_CONTROLLER.md)** - Client-specific advanced queries

## Authentication

All Gateway endpoints require authentication through Keycloak. The Gateway validates JSON Web Tokens (JWT) and forwards user identity information to backend services via headers:

- `X-User-Id`: Authenticated user identifier
- `X-User-Roles`: User roles for authorization

## Request Routing

The Gateway routes requests based on URL patterns:
- `/api/v0/*` - Routes to version 0 endpoints
- `/api/v1/*` - Routes to version 1 endpoints
- `/api/v2/*` - Routes to version 2 endpoints

Each path segment after the version corresponds to a service and operation.

## Related Documentation

- [API Gateway Service](../../../2_BACKEND/1_API_GATEWAY.md) - Service architecture and components
- [API Documentation Overview](../api_documentation.md) - Complete API reference

---

**Navigation**: [← Back to API Documentation](../api_documentation.md) | [↑ APIs and Functions](../../README.md) | [↑ Report Index](../../../README.md)
