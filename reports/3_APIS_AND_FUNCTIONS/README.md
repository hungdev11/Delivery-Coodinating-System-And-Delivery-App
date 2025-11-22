# APIs and Functions Documentation

This folder contains comprehensive API documentation and system workflow diagrams for the Delivery System.

## Table of Contents

- [Structure](#structure)
- [API Documentation](#api-documentation)
- [System Diagrams](#system-diagrams)
- [Quick Reference](#quick-reference)
- [Related Documentation](#related-documentation)

## Structure

The documentation is organized into two main sections:

### [API Documentation](apis/api_documentation.md)

Detailed endpoint documentation organized by service. Each service folder contains versioned controller documentation organized into version subdirectories (v0, v1, v2).

**Services:**
- [API Gateway](apis/api-gateway/README.md) - Proxy endpoints routing to backend services
- [User Service](apis/user-service/README.md) - User management and addresses
- [Parcel Service](apis/parcel-service/README.md) - Parcel lifecycle management
- [Session Service](apis/session-service/README.md) - Delivery session coordination
- [Communication Service](apis/communication-service/README.md) - Real-time messaging and proposals
- [Settings Service](apis/settings-service/README.md) - Configuration management
- [Zone Service](apis/zone-service/README.md) - Geographic data and routing

### [System Diagrams](diagrams/README.md)

Sequence diagrams and state diagrams illustrating system workflows and entity lifecycles.

**Workflow Documents:**
- [Parcel Workflows](diagrams/parcel_workflows.md) - Complete parcel operation workflows
- [Session Workflows](diagrams/session_workflows.md) - Delivery session workflows
- [Routing Workflows](diagrams/routing_workflows.md) - Route calculation workflows
- [System Architecture Flows](diagrams/system_flows.md) - Authentication and data flow patterns

## API Documentation

For complete API endpoint documentation, see [API Documentation Overview](apis/api_documentation.md). This document provides:

- API versioning information (v0, v1, v2)
- Service organization and key endpoints
- Authentication requirements
- Error handling conventions
- Rate limiting information

## System Diagrams

System workflows and entity lifecycles are documented in the [diagrams](diagrams/) folder. These diagrams provide visual context for understanding how different components interact during various operations.

## Quick Reference

### API Overview

- [API Documentation Overview](apis/api_documentation.md) - Complete API reference guide with versioning information

### Finding APIs by Feature

When implementing features, refer to the [Features Documentation](../../features/README.md) which includes API references for each workflow. The API documentation here provides detailed endpoint specifications.

### API Gateway Access

All client applications should access backend services through the API Gateway at port 21500. The Gateway handles authentication and routes requests to appropriate services.

## Related Documentation

- [System Analysis](../../SYSTEM_ANALYSIS.md) - System architecture and technical assessment
- [Backend Services](../../2_BACKEND/) - Service architecture documentation
- [Features Documentation](../../features/README.md) - Feature workflows with API references
- [Client Applications](../../1_CLIENTS/) - Client application documentation
