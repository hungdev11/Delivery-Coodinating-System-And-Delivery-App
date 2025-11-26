# API Documentation: Proposal Config Controller (v1)

**Base Path:** `/api/v1/admin/proposals/configs`

This controller manages proposal type configurations for the Communication Service. Proposal configs define the structure and behavior of interactive proposals used for coordination between users.

## Overview

Proposal configurations define how different types of proposals work in the system. These configurations control the proposal structure, required fields, and validation rules. This controller is used by administrators to manage these configurations.

## Endpoints

| Method | Path | Business Function | Roles Allowed |
|--------|------|-------------------|---------------|
| `GET` | `/` | Get all proposal configurations | `ADMIN` |
| `POST` | `/` | Create a new proposal configuration | `ADMIN` |
| `PUT` | `/{configId}` | Update an existing proposal configuration | `ADMIN` |
| `DELETE` | `/{configId}` | Delete a proposal configuration | `ADMIN` |

## Endpoint Details

### GET /api/v1/admin/proposals/configs

Get all proposal type configurations in the system.

**Response:**
```json
[
  {
    "id": "uuid",
    "type": "POSTPONE_DELIVERY",
    "name": "Postpone Delivery",
    "description": "Request to postpone a delivery",
    "requiredFields": ["parcelId", "assignmentId", "windowStart", "windowEnd"],
    "optionalFields": ["note"],
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
]
```

**Authentication:** Required (Admin role)

### POST /api/v1/admin/proposals/configs

Create a new proposal type configuration.

**Request Body:**
```json
{
  "type": "POSTPONE_DELIVERY",
  "name": "Postpone Delivery",
  "description": "Request to postpone a delivery",
  "requiredFields": ["parcelId", "assignmentId", "windowStart", "windowEnd"],
  "optionalFields": ["note"]
}
```

**Response:**
```json
{
  "id": "uuid",
  "type": "POSTPONE_DELIVERY",
  "name": "Postpone Delivery",
  "description": "Request to postpone a delivery",
  "requiredFields": ["parcelId", "assignmentId", "windowStart", "windowEnd"],
  "optionalFields": ["note"],
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

**Status Code:** 201 Created

**Authentication:** Required (Admin role)

### PUT /api/v1/admin/proposals/configs/{configId}

Update an existing proposal type configuration.

**Path Parameters:**
- `configId` (UUID): The ID of the configuration to update

**Request Body:**
```json
{
  "name": "Postpone Delivery (Updated)",
  "description": "Updated description",
  "requiredFields": ["parcelId", "assignmentId", "windowStart", "windowEnd"],
  "optionalFields": ["note", "reason"]
}
```

**Response:**
```json
{
  "id": "uuid",
  "type": "POSTPONE_DELIVERY",
  "name": "Postpone Delivery (Updated)",
  "description": "Updated description",
  "requiredFields": ["parcelId", "assignmentId", "windowStart", "windowEnd"],
  "optionalFields": ["note", "reason"],
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z"
}
```

**Authentication:** Required (Admin role)

### DELETE /api/v1/admin/proposals/configs/{configId}

Delete a proposal type configuration.

**Path Parameters:**
- `configId` (UUID): The ID of the configuration to delete

**Response:** 204 No Content

**Authentication:** Required (Admin role)

## Related Documentation

- [Proposal Controller](COMMUNICATION_SERVICE_V1_PROPOSAL_CONTROLLER.md) - Proposal operations
- [Communication Service Architecture](../../../../2_BACKEND/2_COMMUNICATION_SERVICE.md) - Service architecture

---

**Navigation**: [← Back to Communication Service](README.md) | [↑ APIs and Functions](../../../README.md) | [↑ Report Index](../../../../README.md)
