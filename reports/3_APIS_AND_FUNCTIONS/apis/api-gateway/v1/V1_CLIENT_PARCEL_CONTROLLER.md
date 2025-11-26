# API Documentation: Client Parcel Controller (v1)

**Base Path:** `/api/v1/client/parcels`

This controller provides client-scoped parcel endpoints that automatically scope results to the authenticated user (receiver). These endpoints are proxied through the API Gateway to the Parcel Service.

## Overview

Client-scoped endpoints automatically filter parcels based on the authenticated user's identity. The API Gateway injects `X-User-Id` and `X-User-Roles` headers based on the JWT token, ensuring that clients can only access their own parcel data.

## Endpoints

| Method | Path | Business Function | Roles Allowed |
|--------|------|-------------------|---------------|
| `POST` | `/received` | Get parcels where current user is the receiver | `CLIENT` |
| `POST` | `/{parcelId}/confirm` | Confirm receipt of a parcel | `CLIENT` |

## Endpoint Details

### POST /api/v1/client/parcels/received

Fetch parcels where the current authenticated user is the receiver.

**Request Body:**
```json
{
  "page": 0,
  "size": 20,
  "filters": [],
  "sort": []
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "code": "PARCEL123",
        "status": "SUCCEEDED",
        "receiverId": "user-uuid",
        "senderId": "user-uuid",
        "createdAt": "2024-01-01T00:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

**Authentication:** Required (JWT token)

### POST /api/v1/client/parcels/{parcelId}/confirm

Confirm receipt of a parcel by the client (receiver).

**Path Parameters:**
- `parcelId` (UUID): The ID of the parcel to confirm

**Request Body:**
```json
{
  "note": "Received successfully",
  "confirmationCode": "optional-code"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "code": "PARCEL123",
    "status": "CONFIRMED",
    "confirmedAt": "2024-01-01T00:00:00Z"
  }
}
```

**Authentication:** Required (JWT token)

## Related Documentation

- [Parcel Service Client Controller](../../parcel-service/v1/PARCEL_SERVICE_V1_CLIENT_PARCEL_CONTROLLER.md) - Backend service implementation
- [Parcel Service Architecture](../../../../2_BACKEND/3_PARCEL_SERVICE.md) - Service architecture

---

**Navigation**: [← Back to API Gateway](README.md) | [↑ APIs and Functions](../../../README.md) | [↑ Report Index](../../../../README.md)
