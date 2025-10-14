# Zone Routes (Proxied)

Base URL: `http://localhost:8080/api/v1`

> **Note**: These endpoints are proxied to the Zone Service. All requests are forwarded to `http://zone-service:21503/api/v1`.

## Overview
The API Gateway provides proxy endpoints for managing delivery zones through the Zone Service. All zone-related operations require authentication unless otherwise specified.

---

## Endpoints

### GET /zone/health
**Check Zone Service health status**

- Description: Verify that the Zone Service is running and accessible (public).
- Response 200:
```json
{
  "result": {
    "status": "ok",
    "service": "zone-service"
  }
}
```

---

### GET /zones
**List all zones (paginated)**

Retrieve a paginated list of delivery zones with optional filtering.

**Query Parameters:**
- `page` (optional) - Page number (default: 0)
- `size` (optional) - Page size (default: 10)
- `search` (optional) - Search term for name/code
- `code` (optional) - Filter by zone code
- `centerId` (optional) - Filter by center ID
- `filters` (optional) - Additional filters array
- `sorts` (optional) - Sorting criteria array
- `selected` (optional) - Selected items array

**Example Request:**
```
GET /api/v1/zones?page=0&size=10&search=HCM
```

**Response 200:**
```json
{
  "result": {
    "data": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "code": "ZONE001",
        "name": "District 1 Zone",
        "polygon": null,
        "centerId": "660e8400-e29b-41d4-a716-446655440001",
        "centerCode": "CTR001",
        "centerName": "Central Distribution Center"
      }
    ],
    "page": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1,
      "filters": [],
      "sorts": [],
      "selected": []
    }
  }
}
```

---

### GET /zones/{id}
**Get zone by ID**

Retrieve detailed information about a specific zone by its UUID.

**Parameters:**
- `id` (path) - Zone UUID

**Response 200:**
```json
{
  "result": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "code": "ZONE001",
    "name": "District 1 Zone",
    "polygon": {
      "type": "Polygon",
      "coordinates": [[[106.660172, 10.762622], [106.670000, 10.770000], [106.665000, 10.775000], [106.660172, 10.762622]]]
    },
    "centerId": "660e8400-e29b-41d4-a716-446655440001",
    "centerCode": "CTR001",
    "centerName": "Central Distribution Center"
  }
}
```

**Errors:**
- 404 - Zone not found

---

### GET /zones/code/{code}
**Get zone by code**

Retrieve zone information using its unique code identifier.

**Parameters:**
- `code` (path) - Zone code (e.g., "ZONE001")

**Response 200:**
```json
{
  "result": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "code": "ZONE001",
    "name": "District 1 Zone",
    "polygon": null,
    "centerId": "660e8400-e29b-41d4-a716-446655440001",
    "centerCode": "CTR001",
    "centerName": "Central Distribution Center"
  }
}
```

**Errors:**
- 404 - Zone not found

---

### GET /zones/center/{centerId}
**Get zones by distribution center**

Retrieve all zones associated with a specific distribution center.

**Parameters:**
- `centerId` (path) - Distribution center UUID

**Response 200:**
```json
{
  "result": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "code": "ZONE001",
      "name": "District 1 Zone",
      "polygon": null,
      "centerId": "660e8400-e29b-41d4-a716-446655440001",
      "centerCode": "CTR001",
      "centerName": "Central Distribution Center"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "code": "ZONE002",
      "name": "District 2 Zone",
      "polygon": null,
      "centerId": "660e8400-e29b-41d4-a716-446655440001",
      "centerCode": "CTR001",
      "centerName": "Central Distribution Center"
    }
  ]
}
```

**Errors:**
- 404 - Center not found

---

### POST /zones
**Create new zone**

Create a new delivery zone associated with a distribution center.

**Request Body:**
```json
{
  "code": "ZONE003",
  "name": "District 3 Zone",
  "polygon": {
    "type": "Polygon",
    "coordinates": [[[106.660172, 10.762622], [106.670000, 10.770000], [106.665000, 10.775000], [106.660172, 10.762622]]]
  },
  "centerId": "660e8400-e29b-41d4-a716-446655440001"
}
```

**Field Descriptions:**
- `code` (required) - Unique zone code
- `name` (required) - Zone display name
- `polygon` (optional) - GeoJSON polygon defining zone boundaries
- `centerId` (required) - UUID of the parent distribution center

**Response 201:**
```json
{
  "result": {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "code": "ZONE003",
    "name": "District 3 Zone",
    "polygon": {
      "type": "Polygon",
      "coordinates": [[[106.660172, 10.762622], [106.670000, 10.770000], [106.665000, 10.775000], [106.660172, 10.762622]]]
    },
    "centerId": "660e8400-e29b-41d4-a716-446655440001",
    "centerCode": "CTR001",
    "centerName": "Central Distribution Center"
  },
  "message": "Zone created successfully"
}
```

**Errors:**
- 400 - Zone with this code already exists
- 404 - Distribution center not found
- 400 - Validation failed (missing required fields)

---

### PUT /zones/{id}
**Update existing zone**

Update zone details. All fields are optional - only provided fields will be updated.

**Parameters:**
- `id` (path) - Zone UUID

**Request Body:**
```json
{
  "code": "ZONE003_UPDATED",
  "name": "Updated District 3 Zone",
  "polygon": {
    "type": "Polygon",
    "coordinates": [[[106.660172, 10.762622], [106.670000, 10.770000], [106.665000, 10.775000], [106.660172, 10.762622]]]
  },
  "centerId": "660e8400-e29b-41d4-a716-446655440001"
}
```

**Response 200:**
```json
{
  "result": {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "code": "ZONE003_UPDATED",
    "name": "Updated District 3 Zone",
    "polygon": {
      "type": "Polygon",
      "coordinates": [[[106.660172, 10.762622], [106.670000, 10.770000], [106.665000, 10.775000], [106.660172, 10.762622]]]
    },
    "centerId": "660e8400-e29b-41d4-a716-446655440001",
    "centerCode": "CTR001",
    "centerName": "Central Distribution Center"
  },
  "message": "Zone updated successfully"
}
```

**Errors:**
- 404 - Zone not found
- 404 - Distribution center not found (if centerId is updated)
- 400 - Zone code already exists (if code is updated to duplicate)

---

### DELETE /zones/{id}
**Delete zone**

Permanently delete a delivery zone.

**Parameters:**
- `id` (path) - Zone UUID

**Response 204:**
```
No Content
```

**Alternative Response 200:**
```json
{
  "result": null,
  "message": "Zone deleted successfully"
}
```

**Errors:**
- 404 - Zone not found

---

## Data Models

### Zone Object
```typescript
{
  id: string;              // UUID
  code: string;            // Unique zone code (e.g., "ZONE001")
  name: string;            // Display name
  polygon: GeoJSON | null; // Zone boundary polygon
  centerId: string;        // Parent distribution center UUID
  centerCode: string;      // Parent center code (read-only)
  centerName: string;      // Parent center name (read-only)
}
```

### Polygon (GeoJSON)
```typescript
{
  type: "Polygon";
  coordinates: number[][][]; // Array of [longitude, latitude] pairs
}
```

---

## Examples

### Example 1: List zones for a specific center
```bash
GET /api/v1/zones?centerId=660e8400-e29b-41d4-a716-446655440001&page=0&size=20
```

### Example 2: Search zones by name
```bash
GET /api/v1/zones?search=District&page=0&size=10
```

### Example 3: Create a zone with polygon
```bash
POST /api/v1/zones
Content-Type: application/json

{
  "code": "HCM_D1",
  "name": "Ho Chi Minh District 1",
  "polygon": {
    "type": "Polygon",
    "coordinates": [
      [
        [106.695, 10.776],
        [106.705, 10.776],
        [106.705, 10.785],
        [106.695, 10.785],
        [106.695, 10.776]
      ]
    ]
  },
  "centerId": "660e8400-e29b-41d4-a716-446655440001"
}
```

### Example 4: Update zone name only
```bash
PUT /api/v1/zones/550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{
  "name": "District 1 Premium Zone"
}
```

---

## Authorization

All zone endpoints require authentication via JWT token, except for `/zone/health`.

**Required Headers:**
```
Authorization: Bearer <jwt_token>
```

**Required Roles:**
- Read operations (GET): Any authenticated user
- Write operations (POST, PUT, DELETE): ADMIN or MANAGER role

---

## Notes

1. **Polygon Format**: Polygons use GeoJSON format with coordinates in `[longitude, latitude]` order
2. **Zone Codes**: Must be unique across the entire system
3. **Center Relationship**: Zones must be associated with an existing distribution center
4. **Pagination**: Default page size is 10, maximum is 100
5. **Service Availability**: If Zone Service is unavailable, endpoints will return 503 Service Unavailable
6. **Async Processing**: All requests are processed asynchronously through the gateway
