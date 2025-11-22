# API Documentation: Client Parcel Controller (v1)

**Base Path:** `/api/v1/client/parcels`

This controller provides client-scoped parcel endpoints that automatically scope results to the authenticated user (receiver) based on the `X-User-Id` header forwarded from the API Gateway.

## Overview

Client-scoped endpoints automatically filter parcels based on the authenticated user's identity. The API Gateway forwards the `X-User-Id` header extracted from the JWT token, ensuring that clients can only access their own parcel data.

## Endpoints

| Method | Path | Business Function | Headers Required |
|--------|------|-------------------|------------------|
| `POST` | `/received` | Get parcels where current user is the receiver | `X-User-Id` |
| `POST` | `/{parcelId}/confirm` | Confirm receipt of a parcel | `X-User-Id` |

## Endpoint Details

### POST /api/v1/client/parcels/received

Fetch parcels where the current authenticated user is the receiver. Supports V2 pagination and filtering.

**Headers:**
- `X-User-Id` (required): User ID extracted from JWT by API Gateway

**Request Body:**
```json
{
  "page": 0,
  "size": 20,
  "filters": [
    {
      "field": "status",
      "operator": "EQUALS",
      "value": "SUCCEEDED"
    }
  ],
  "sort": [
    {
      "field": "createdAt",
      "direction": "DESC"
    }
  ]
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
        "createdAt": "2024-01-01T00:00:00Z",
        "deliveredAt": "2024-01-01T12:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### POST /api/v1/client/parcels/{parcelId}/confirm

Confirm receipt of a parcel by the client (receiver). This endpoint validates that the authenticated user is the receiver of the parcel.

**Path Parameters:**
- `parcelId` (UUID): The ID of the parcel to confirm

**Headers:**
- `X-User-Id` (required): User ID extracted from JWT by API Gateway

**Request Body:**
```json
{
  "note": "Received successfully",
  "confirmationCode": "optional-code-if-provided-by-shipper"
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
    "confirmedAt": "2024-01-01T00:00:00Z",
    "confirmationNote": "Received successfully"
  }
}
```

**Validation:**
- The parcel must exist
- The authenticated user must be the receiver of the parcel
- The parcel status must be `SUCCEEDED` (delivered by shipper)

## Related Documentation

- [API Gateway Client Parcel Controller](../../api-gateway/v1/V1_CLIENT_PARCEL_CONTROLLER.md) - Gateway proxy endpoints
- [Parcel Service Architecture](../../../../2_BACKEND/3_PARCEL_SERVICE.md) - Service architecture

---

**Navigation**: [← Back to Parcel Service](../README.md) | [↑ APIs and Functions](../../../README.md) | [↑ Report Index](../../../../README.md)
