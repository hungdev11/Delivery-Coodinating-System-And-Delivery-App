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

### POST /zones
**List zones with advanced filtering and sorting**

Retrieve a paginated list of delivery zones with comprehensive filtering, sorting, and search capabilities using the new POST endpoint standard.

**Request Body (PagingRequest):**
```json
{
  "page": 0,
  "size": 10,
  "search": "HCM",
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "centerId",
        "operator": "eq",
        "value": "660e8400-e29b-41d4-a716-446655440001"
      },
      {
        "field": "name",
        "operator": "contains",
        "value": "District",
        "caseSensitive": false
      }
    ]
  },
  "sorts": [
    {
      "field": "name",
      "direction": "asc"
    }
  ],
  "selected": []
}
```

**Field Descriptions:**
- `page` (optional) - Page number, default: 0
- `size` (optional) - Page size, default: 10
- `search` (optional) - Global search term
- `filters` (optional) - Advanced filter groups with MongoDB-style conditions
- `sorts` (optional) - Sort configuration array
- `selected` (optional) - Selected item IDs array

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
      "filters": {
        "logic": "AND",
        "conditions": [
          {
            "field": "centerId",
            "operator": "eq",
            "value": "660e8400-e29b-41d4-a716-446655440001"
          }
        ]
      },
      "sorts": [
        {
          "field": "name",
          "direction": "asc"
        }
      ],
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

### POST /zones/create
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

### GET /zones/filterable-fields
**Get filterable fields for zones**

Retrieve a list of fields that can be used in filter conditions.

**Response 200:**
```json
{
  "result": [
    "id",
    "code", 
    "name",
    "centerId",
    "centerCode",
    "centerName",
    "createdAt",
    "updatedAt"
  ]
}
```

---

### GET /zones/sortable-fields
**Get sortable fields for zones**

Retrieve a list of fields that can be used for sorting.

**Response 200:**
```json
{
  "result": [
    "id",
    "code",
    "name", 
    "centerId",
    "centerCode",
    "centerName",
    "createdAt",
    "updatedAt"
  ]
}
```

---

## Advanced Filtering

The POST `/zones` endpoint supports comprehensive filtering using MongoDB-style filter groups.

### Filter Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `eq` | Equals | `{"field": "centerId", "operator": "eq", "value": "uuid"}` |
| `ne` | Not equals | `{"field": "code", "operator": "ne", "value": "ZONE001"}` |
| `contains` | String contains | `{"field": "name", "operator": "contains", "value": "District"}` |
| `startsWith` | String starts with | `{"field": "code", "operator": "startsWith", "value": "ZONE"}` |
| `endsWith` | String ends with | `{"field": "code", "operator": "endsWith", "value": "001"}` |
| `gt` | Greater than | `{"field": "createdAt", "operator": "gt", "value": "2024-01-01"}` |
| `gte` | Greater than or equal | `{"field": "createdAt", "operator": "gte", "value": "2024-01-01"}` |
| `lt` | Less than | `{"field": "createdAt", "operator": "lt", "value": "2024-12-31"}` |
| `lte` | Less than or equal | `{"field": "createdAt", "operator": "lte", "value": "2024-12-31"}` |
| `between` | Between values | `{"field": "createdAt", "operator": "between", "value": ["2024-01-01", "2024-12-31"]}` |
| `in` | In array | `{"field": "centerId", "operator": "in", "value": ["uuid1", "uuid2"]}` |
| `notIn` | Not in array | `{"field": "code", "operator": "notIn", "value": ["ZONE001", "ZONE002"]}` |
| `isNull` | Is null | `{"field": "polygon", "operator": "isNull"}` |
| `isNotNull` | Is not null | `{"field": "polygon", "operator": "isNotNull"}` |

### Filter Examples

#### Basic Filter
```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "centerId",
        "operator": "eq",
        "value": "660e8400-e29b-41d4-a716-446655440001"
      }
    ]
  }
}
```

#### Complex Nested Filter
```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "name",
        "operator": "contains",
        "value": "District",
        "caseSensitive": false
      },
      {
        "logic": "OR",
        "conditions": [
          {
            "field": "centerId",
            "operator": "in",
            "value": ["uuid1", "uuid2"]
          },
          {
            "field": "code",
            "operator": "startsWith",
            "value": "HCM"
          }
        ]
      }
    ]
  }
}
```

#### Date Range Filter
```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "createdAt",
        "operator": "between",
        "value": ["2024-01-01", "2024-12-31"]
      }
    ]
  }
}
```

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

### Example 1: List zones for a specific center (POST)
```bash
POST /api/v1/zones
Content-Type: application/json

{
  "page": 0,
  "size": 20,
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "centerId",
        "operator": "eq",
        "value": "660e8400-e29b-41d4-a716-446655440001"
      }
    ]
  }
}
```

### Example 2: Search zones by name (POST)
```bash
POST /api/v1/zones
Content-Type: application/json

{
  "page": 0,
  "size": 10,
  "search": "District"
}
```

### Example 3: Advanced filtering with sorting (POST)
```bash
POST /api/v1/zones
Content-Type: application/json

{
  "page": 0,
  "size": 10,
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "name",
        "operator": "contains",
        "value": "District",
        "caseSensitive": false
      },
      {
        "field": "createdAt",
        "operator": "gte",
        "value": "2024-01-01"
      }
    ]
  },
  "sorts": [
    {
      "field": "name",
      "direction": "asc"
    }
  ]
}
```

### Example 4: Create a zone with polygon
```bash
POST /api/v1/zones/create
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

### Example 5: Update zone name only
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
5. **POST Standard**: List operations now use POST with `PagingRequest` body for advanced filtering
6. **Filter Support**: Comprehensive MongoDB-style filtering with nested conditions and multiple operators
7. **Service Availability**: If Zone Service is unavailable, endpoints will return 503 Service Unavailable
8. **Async Processing**: All requests are processed asynchronously through the gateway
9. **Migration**: GET `/zones` is deprecated, use POST `/zones` with `PagingRequest` body instead
