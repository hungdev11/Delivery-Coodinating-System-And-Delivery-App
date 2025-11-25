**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](README.md)

---

# API Documentation

This folder contains detailed API endpoint documentation organized by service. Each service folder includes versioned controller documentation.

## Service Organization

APIs are organized by service to facilitate easy reference and maintenance:

### [API Gateway](api-gateway/)
Proxy endpoints that route requests to backend services. Gateway endpoints mirror backend service endpoints with version prefixes (v0, v1, v2).

**Key Features:**
- Authentication and authorization handling
- Request routing to appropriate microservices
- Request transformation and header injection
- Version management

### [User Service](user-service/)
User management, delivery personnel profiles, and address management APIs.

**Key Endpoints:**
- User CRUD operations
- Address management
- Delivery personnel profiles
- Keycloak synchronization

### [Parcel Service](parcel-service/)
Parcel lifecycle management from creation through delivery confirmation.

**Key Endpoints:**
- Parcel CRUD operations
- Status management
- Confirmation workflows
- Dispute handling

### [Session Service](session-service/)
Delivery session coordination and parcel assignment management.

**Key Endpoints:**
- Session management
- Assignment operations
- QR code generation
- Status tracking

### [Communication Service](communication-service/)
Real-time messaging, interactive proposals, and notifications.

**Key Endpoints:**
- Conversation management
- Message operations
- Interactive proposals
- Notification delivery

### [Settings Service](settings-service/)
Centralized configuration management.

**Key Endpoints:**
- Setting CRUD operations
- Bulk operations
- Health checks

### [Zone Service](zone-service/)
Geographic data, delivery zones, and route calculation.

**Key Endpoints:**
- Zone management
- Address operations
- Route calculation
- OSRM integration

## API Versioning

Each service supports multiple API versions:

- **Version 0 (v0)**: Basic functionality, initial implementation
- **Version 1 (v1)**: Enhanced features with improved functionality
- **Version 2 (v2)**: Advanced features including complex filtering and querying

## Accessing APIs

All client applications should access backend services through the API Gateway at port 21500. The Gateway handles authentication and routes requests to appropriate services.

Direct access to individual microservices is not recommended for production use.

## Related Documentation

- [API Documentation Overview](../api_documentation.md) - Complete API reference guide
- [System Diagrams](../diagrams/README.md) - Sequence and state diagrams
- [Features Documentation](../../features/README.md) - Feature documentation with API references


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](README.md)






