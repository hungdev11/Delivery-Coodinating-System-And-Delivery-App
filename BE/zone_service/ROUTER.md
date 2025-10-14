# Zone Service API Documentation

## Base URL
```
http://localhost:21503/api/v1
```

## Response Format

All responses follow the standard `BaseResponse<T>` format:

```typescript
{
  result?: T;        // Data payload (null for errors)
  message?: string;  // Success or error message
}
```

---

## Health Check Endpoints

### Basic Health Check
```http
GET /health
```

**Response:** `200 OK`
```json
{
  "status": "UP",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "service": "zone-service"
}
```

### Detailed Health Check
```http
GET /health/detailed
```

**Response:** `200 OK` or `503 Service Unavailable`
```json
{
  "status": "UP",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "service": "zone-service",
  "dependencies": {
    "database": { "status": "UP" },
    "kafka": { "status": "UP" },
    "settingsService": { "status": "UP" }
  }
}
```

### Readiness Check
```http
GET /health/readiness
```

**Response:** `200 OK` or `503 Service Unavailable`
```json
{
  "status": "READY"
}
```

### Liveness Check
```http
GET /health/liveness
```

**Response:** `200 OK`
```json
{
  "status": "ALIVE"
}
```

---

## Center Endpoints

### 1. Get All Centers (Paginated)

```http
GET /api/v1/centers?page=0&size=10&search=keyword&code=CTR001
```

**Query Parameters:**
```typescript
{
  page?: number;      // Page number (default: 0)
  size?: number;      // Page size (default: 10)
  search?: string;    // Search by name, code, or address
  code?: string;      // Filter by exact code
  filters?: any[];    // Additional filters
  sorts?: any[];      // Sort configuration
  selected?: string[]; // Selected IDs
}
```

**Response:** `200 OK`
```json
{
  "result": {
    "data": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "code": "CTR001",
        "name": "Center Name",
        "address": "123 Main Street",
        "lat": 10.762622,
        "lon": 106.660172,
        "polygon": {
          "type": "Polygon",
          "coordinates": [...]
        }
      }
    ],
    "page": {
      "page": 0,
      "size": 10,
      "totalElements": 100,
      "totalPages": 10,
      "filters": [],
      "sorts": [],
      "selected": []
    }
  }
}
```

### 2. Get Center by ID

```http
GET /api/v1/centers/:id
```

**Path Parameters:**
- `id` (string, required): Center UUID

**Response:** `200 OK`
```json
{
  "result": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "code": "CTR001",
    "name": "Center Name",
    "address": "123 Main Street",
    "lat": 10.762622,
    "lon": 106.660172,
    "polygon": null
  }
}
```

**Error Response:** `404 Not Found`
```json
{
  "message": "Center not found"
}
```

### 3. Get Center by Code

```http
GET /api/v1/centers/code/:code
```

**Path Parameters:**
- `code` (string, required): Center code

**Response:** `200 OK`
```json
{
  "result": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "code": "CTR001",
    "name": "Center Name",
    "address": "123 Main Street",
    "lat": 10.762622,
    "lon": 106.660172,
    "polygon": null
  }
}
```

**Error Response:** `404 Not Found`
```json
{
  "message": "Center not found"
}
```

### 4. Create Center

```http
POST /api/v1/centers
Content-Type: application/json
```

**Request Body:**
```typescript
{
  code: string;        // Required, unique
  name: string;        // Required
  address?: string;    // Optional
  lat?: number;        // Optional, latitude
  lon?: number;        // Optional, longitude
  polygon?: object;    // Optional, GeoJSON polygon
}
```

**Example:**
```json
{
  "code": "CTR001",
  "name": "Central Distribution Center",
  "address": "123 Main Street, District 1, Ho Chi Minh City",
  "lat": 10.762622,
  "lon": 106.660172,
  "polygon": {
    "type": "Polygon",
    "coordinates": [
      [
        [106.660172, 10.762622],
        [106.670172, 10.762622],
        [106.670172, 10.772622],
        [106.660172, 10.772622],
        [106.660172, 10.762622]
      ]
    ]
  }
}
```

**Response:** `201 Created`
```json
{
  "result": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "code": "CTR001",
    "name": "Central Distribution Center",
    "address": "123 Main Street, District 1, Ho Chi Minh City",
    "lat": 10.762622,
    "lon": 106.660172,
    "polygon": { ... }
  },
  "message": "Center created successfully"
}
```

**Error Response:** `400 Bad Request`
```json
{
  "message": "Center with this code already exists"
}
```

### 5. Update Center

```http
PUT /api/v1/centers/:id
Content-Type: application/json
```

**Path Parameters:**
- `id` (string, required): Center UUID

**Request Body:** (all fields optional)
```typescript
{
  code?: string;
  name?: string;
  address?: string;
  lat?: number;
  lon?: number;
  polygon?: object;
}
```

**Example:**
```json
{
  "name": "Updated Center Name",
  "address": "456 New Address"
}
```

**Response:** `200 OK`
```json
{
  "result": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "code": "CTR001",
    "name": "Updated Center Name",
    "address": "456 New Address",
    "lat": 10.762622,
    "lon": 106.660172,
    "polygon": null
  },
  "message": "Center updated successfully"
}
```

**Error Responses:**
- `404 Not Found`: Center not found
- `400 Bad Request`: Code already exists (if updating code)

### 6. Delete Center

```http
DELETE /api/v1/centers/:id
```

**Path Parameters:**
- `id` (string, required): Center UUID

**Response:** `200 OK`
```json
{
  "result": null,
  "message": "Center deleted successfully"
}
```

**Error Response:** `404 Not Found`
```json
{
  "message": "Center not found"
}
```

---

## Zone Endpoints

### 1. Get All Zones (Paginated)

```http
GET /api/v1/zones?page=0&size=10&search=keyword&code=ZONE001&centerId=uuid
```

**Query Parameters:**
```typescript
{
  page?: number;       // Page number (default: 0)
  size?: number;       // Page size (default: 10)
  search?: string;     // Search by name or code
  code?: string;       // Filter by exact code
  centerId?: string;   // Filter by center ID
  filters?: any[];     // Additional filters
  sorts?: any[];       // Sort configuration
  selected?: string[]; // Selected IDs
}
```

**Response:** `200 OK`
```json
{
  "result": {
    "data": [
      {
        "id": "660e8400-e29b-41d4-a716-446655440000",
        "code": "ZONE001",
        "name": "Zone District 1",
        "polygon": {
          "type": "Polygon",
          "coordinates": [...]
        },
        "centerId": "550e8400-e29b-41d4-a716-446655440000",
        "centerCode": "CTR001",
        "centerName": "Central Distribution Center"
      }
    ],
    "page": {
      "page": 0,
      "size": 10,
      "totalElements": 50,
      "totalPages": 5,
      "filters": [],
      "sorts": [],
      "selected": []
    }
  }
}
```

### 2. Get Zone by ID

```http
GET /api/v1/zones/:id
```

**Path Parameters:**
- `id` (string, required): Zone UUID

**Response:** `200 OK`
```json
{
  "result": {
    "id": "660e8400-e29b-41d4-a716-446655440000",
    "code": "ZONE001",
    "name": "Zone District 1",
    "polygon": {
      "type": "Polygon",
      "coordinates": [...]
    },
    "centerId": "550e8400-e29b-41d4-a716-446655440000",
    "centerCode": "CTR001",
    "centerName": "Central Distribution Center"
  }
}
```

**Error Response:** `404 Not Found`
```json
{
  "message": "Zone not found"
}
```

### 3. Get Zone by Code

```http
GET /api/v1/zones/code/:code
```

**Path Parameters:**
- `code` (string, required): Zone code

**Response:** `200 OK`
```json
{
  "result": {
    "id": "660e8400-e29b-41d4-a716-446655440000",
    "code": "ZONE001",
    "name": "Zone District 1",
    "polygon": null,
    "centerId": "550e8400-e29b-41d4-a716-446655440000",
    "centerCode": "CTR001",
    "centerName": "Central Distribution Center"
  }
}
```

**Error Response:** `404 Not Found`
```json
{
  "message": "Zone not found"
}
```

### 4. Get Zones by Center ID

```http
GET /api/v1/zones/center/:centerId
```

**Path Parameters:**
- `centerId` (string, required): Center UUID

**Response:** `200 OK`
```json
{
  "result": [
    {
      "id": "660e8400-e29b-41d4-a716-446655440000",
      "code": "ZONE001",
      "name": "Zone District 1",
      "polygon": null,
      "centerId": "550e8400-e29b-41d4-a716-446655440000",
      "centerCode": "CTR001",
      "centerName": "Central Distribution Center"
    },
    {
      "id": "770e8400-e29b-41d4-a716-446655440000",
      "code": "ZONE002",
      "name": "Zone District 2",
      "polygon": null,
      "centerId": "550e8400-e29b-41d4-a716-446655440000",
      "centerCode": "CTR001",
      "centerName": "Central Distribution Center"
    }
  ]
}
```

### 5. Create Zone

```http
POST /api/v1/zones
Content-Type: application/json
```

**Request Body:**
```typescript
{
  code: string;        // Required, unique
  name: string;        // Required
  polygon?: object;    // Optional, GeoJSON polygon
  centerId: string;    // Required, must exist
}
```

**Example:**
```json
{
  "code": "ZONE001",
  "name": "Zone District 1",
  "centerId": "550e8400-e29b-41d4-a716-446655440000",
  "polygon": {
    "type": "Polygon",
    "coordinates": [
      [
        [106.660172, 10.762622],
        [106.665172, 10.762622],
        [106.665172, 10.767622],
        [106.660172, 10.767622],
        [106.660172, 10.762622]
      ]
    ]
  }
}
```

**Response:** `201 Created`
```json
{
  "result": {
    "id": "660e8400-e29b-41d4-a716-446655440000",
    "code": "ZONE001",
    "name": "Zone District 1",
    "polygon": { ... },
    "centerId": "550e8400-e29b-41d4-a716-446655440000",
    "centerCode": "CTR001",
    "centerName": "Central Distribution Center"
  },
  "message": "Zone created successfully"
}
```

**Error Responses:**
- `400 Bad Request`: Zone with this code already exists
- `404 Not Found`: Center not found

### 6. Update Zone

```http
PUT /api/v1/zones/:id
Content-Type: application/json
```

**Path Parameters:**
- `id` (string, required): Zone UUID

**Request Body:** (all fields optional)
```typescript
{
  code?: string;
  name?: string;
  polygon?: object;
  centerId?: string;   // Must exist if provided
}
```

**Example:**
```json
{
  "name": "Updated Zone Name",
  "centerId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:** `200 OK`
```json
{
  "result": {
    "id": "660e8400-e29b-41d4-a716-446655440000",
    "code": "ZONE001",
    "name": "Updated Zone Name",
    "polygon": null,
    "centerId": "550e8400-e29b-41d4-a716-446655440000",
    "centerCode": "CTR001",
    "centerName": "Central Distribution Center"
  },
  "message": "Zone updated successfully"
}
```

**Error Responses:**
- `404 Not Found`: Zone or Center not found
- `400 Bad Request`: Code already exists (if updating code)

### 7. Delete Zone

```http
DELETE /api/v1/zones/:id
```

**Path Parameters:**
- `id` (string, required): Zone UUID

**Response:** `200 OK`
```json
{
  "result": null,
  "message": "Zone deleted successfully"
}
```

**Error Response:** `404 Not Found`
```json
{
  "message": "Zone not found"
}
```

---

## TypeScript Types

### Base Response Types

```typescript
// Base response wrapper
class BaseResponse<T> {
  result?: T;
  message?: string;
}

// Paginated data wrapper
class PagedData<TData extends { id: string }> {
  data: TData[];
  page: Paging<TData['id']>;
}

// Pagination info
class Paging<TKey> {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  filters: any[];
  sorts: any[];
  selected: TKey[];
}

// Base paging request
class PagingRequest {
  page?: number;
  size?: number;
  filters?: any[];
  sorts?: any[];
  selected?: string[];
}
```

### Center Types

```typescript
// Center DTO
interface CenterDto {
  id: string;
  code: string;
  name: string;
  address?: string | null;
  lat?: number | null;
  lon?: number | null;
  polygon?: any | null;
}

// Create Center Request
interface CreateCenterDto {
  code: string;
  name: string;
  address?: string;
  lat?: number;
  lon?: number;
  polygon?: any;
}

// Update Center Request
interface UpdateCenterDto {
  code?: string;
  name?: string;
  address?: string;
  lat?: number;
  lon?: number;
  polygon?: any;
}

// Center Paging Request
interface CenterPagingRequest extends PagingRequest {
  search?: string;
  code?: string;
}
```

### Zone Types

```typescript
// Zone DTO
interface ZoneDto {
  id: string;
  code: string;
  name: string;
  polygon?: any | null;
  centerId: string;
  centerCode?: string;
  centerName?: string;
}

// Create Zone Request
interface CreateZoneDto {
  code: string;
  name: string;
  polygon?: any;
  centerId: string;
}

// Update Zone Request
interface UpdateZoneDto {
  code?: string;
  name?: string;
  polygon?: any;
  centerId?: string;
}

// Zone Paging Request
interface ZonePagingRequest extends PagingRequest {
  search?: string;
  code?: string;
  centerId?: string;
}
```

---

## Error Responses

All error responses follow the same format:

```json
{
  "message": "Error description"
}
```

### Common HTTP Status Codes

- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request data or validation failed
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error
- `503 Service Unavailable` - Service dependency unavailable

---

## Notes

1. All IDs are UUIDs (v4)
2. All timestamps are in ISO 8601 format
3. Pagination is zero-based (first page = 0)
4. Default page size is 10
5. GeoJSON polygons follow [RFC 7946](https://datatracker.ietf.org/doc/html/rfc7946) specification
6. Coordinates are in [longitude, latitude] order (GeoJSON standard)
7. All string searches are case-insensitive and use partial matching

---

## Examples

### Example: Create Center and Zone

**Step 1: Create Center**
```bash
curl -X POST http://localhost:21503/api/v1/centers \
  -H "Content-Type: application/json" \
  -d '{
    "code": "CTR001",
    "name": "Central Hub",
    "address": "123 Main St",
    "lat": 10.762622,
    "lon": 106.660172
  }'
```

**Step 2: Create Zone for Center**
```bash
curl -X POST http://localhost:21503/api/v1/zones \
  -H "Content-Type: application/json" \
  -d '{
    "code": "ZONE001",
    "name": "District 1 Zone",
    "centerId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

### Example: Search and Filter

**Search centers by name:**
```bash
curl "http://localhost:21503/api/v1/centers?search=central&page=0&size=20"
```

**Get zones for specific center:**
```bash
curl "http://localhost:21503/api/v1/zones/center/550e8400-e29b-41d4-a716-446655440000"
```

**Filter zones by center with pagination:**
```bash
curl "http://localhost:21503/api/v1/zones?centerId=550e8400-e29b-41d4-a716-446655440000&page=0&size=10"
```
