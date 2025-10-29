# Address API Documentation

Base path: `/api/v1/addresses`

## Overview

The Address API provides endpoints for managing addresses (points of interest) and their associations with road segments. Addresses are automatically:
- Associated with the nearest road segment (parent curve)
- Projected onto the segment curve
- Indexed with geohash for fast proximity searches
- Stored with PostGIS geometry for accurate distance calculations

## Key Features

- **Automatic Segment Association**: Finds nearest road segment within 500m
- **Projection Calculation**: Projects address onto segment curve with position (0.0-1.0)
- **Geohash Indexing**: Enables fast proximity pre-filtering (~76m precision)
- **PostGIS Integration**: Accurate distance calculations using geography type
- **Batch Import**: Efficiently import many addresses at once

## Endpoints

### 1. Create Address

Create a new address with automatic segment association and projection.

**Endpoint**: `POST /api/v1/addresses`

**Request Body**:
```json
{
  "name": "Học viện Công nghệ Bưu chính Viễn thông",
  "nameEn": "Posts and Telecommunications Institute of Technology",
  "addressText": "Km 10, Đường Nguyễn Trãi, Hà Đông, Hà Nội",
  "lat": 10.8505,
  "lon": 106.7717,
  "addressType": "SCHOOL",
  "segmentId": "optional-segment-id",  // Optional: manually specify segment
  "zoneId": "optional-zone-id",        // Optional: manually specify zone
  "wardName": "Tăng Nhơn Phú A",
  "districtName": "Thủ Đức"
}
```

**Response** (201):
```json
{
  "success": true,
  "message": "Address created successfully",
  "data": {
    "id": "addr-123",
    "name": "Học viện Công nghệ Bưu chính Viễn thông",
    "nameEn": "Posts and Telecommunications Institute of Technology",
    "addressText": "Km 10, Đường Nguyễn Trãi, Hà Đông, Hà Nội",
    "lat": 10.8505,
    "lon": 106.7717,
    "geohash": "w3gv2h7",
    "segmentId": "segment-456",
    "segmentName": "Đường Nguyễn Trãi",
    "roadType": "PRIMARY",
    "segmentPosition": 0.6234,          // Position along segment (0.0-1.0)
    "distanceToSegment": 12.5,          // Perpendicular distance (meters)
    "projectedLat": 10.8506,            // Projected point on segment
    "projectedLon": 106.7718,
    "zoneId": "zone-789",
    "zoneName": "North Zone",
    "wardName": "Tăng Nhơn Phú A",
    "districtName": "Thủ Đức",
    "addressType": "SCHOOL",
    "createdAt": "2025-10-14T10:00:00Z",
    "updatedAt": "2025-10-14T10:00:00Z"
  }
}
```

**Address Types**:
- `GENERAL` - General address
- `SCHOOL` - School
- `HOSPITAL` - Hospital
- `GOVERNMENT` - Government office
- `SHOPPING` - Shopping center
- `RESTAURANT` - Restaurant
- `HOTEL` - Hotel
- `BANK` - Bank
- `GAS_STATION` - Gas station
- `PARKING` - Parking lot
- `BUS_STOP` - Bus stop
- `LANDMARK` - Famous landmark

---

### 2. Get Address by ID

Retrieve a specific address by its ID.

**Endpoint**: `GET /api/v1/addresses/:id`

**Response** (200):
```json
{
  "success": true,
  "data": {
    "id": "addr-123",
    "name": "Học viện PTIT",
    // ... (same as create response)
  }
}
```

---

### 3. List Addresses

List addresses with pagination and filters.

**Endpoint**: `GET /api/v1/addresses`

**Query Parameters**:
- `page` (number, default: 1): Page number
- `limit` (number, default: 20, max: 100): Items per page
- `search` (string): Search by name or address text
- `addressType` (string): Filter by address type
- `segmentId` (string): Filter by road segment
- `zoneId` (string): Filter by zone
- `wardName` (string): Filter by ward
- `districtName` (string): Filter by district

**Example**: `GET /api/v1/addresses?page=1&limit=20&search=PTIT&addressType=SCHOOL`

**Response** (200):
```json
{
  "success": true,
  "data": [
    {
      "id": "addr-123",
      "name": "Học viện PTIT",
      // ... address data
    }
  ],
  "paging": {
    "page": 1,
    "limit": 20,
    "total": 150,
    "totalPages": 8
  }
}
```

---

### 4. Find Nearest Addresses

Find addresses nearest to a given point using efficient geohash + PostGIS search.

**Endpoint**: `GET /api/v1/addresses/nearest`

**Query Parameters** (required):
- `lat` (number): Latitude of query point
- `lon` (number): Longitude of query point

**Query Parameters** (optional):
- `limit` (number, default: 10, max: 100): Max number of results
- `maxDistance` (number, default: 5000): Max distance in meters (5km default)
- `addressType` (string): Filter by address type
- `segmentId` (string): Filter by road segment
- `zoneId` (string): Filter by zone

**Example**: `GET /api/v1/addresses/nearest?lat=10.8505&lon=106.7717&limit=5&maxDistance=1000&addressType=SCHOOL`

**Response** (200):
```json
{
  "success": true,
  "data": [
    {
      "id": "addr-123",
      "name": "Học viện PTIT",
      // ... address data
      "distance": 125.5,      // Distance from query point (meters)
      "bearing": 45.2         // Bearing from query point (degrees, 0-360)
    },
    {
      "id": "addr-124",
      "name": "Trường ĐH Bách Khoa",
      "distance": 850.3,
      "bearing": 120.5
    }
  ],
  "meta": {
    "queryPoint": { "lat": 10.8505, "lon": 106.7717 },
    "count": 2
  }
}
```

**Algorithm**:
1. Calculate geohash for query point (precision 7 = ~76m cells)
2. Get 9 geohash cells (center + 8 neighbors) for pre-filtering
3. Use PostGIS `ST_DWithin` for accurate distance filtering
4. Calculate exact distances using geography type
5. Sort by distance and return top N results

**Performance**: ~5-10ms for 10,000 addresses using geohash pre-filter + PostGIS

---

### 5. Update Address

Update an address. Location changes trigger automatic re-calculation of segment association and projection.

**Endpoint**: `PUT /api/v1/addresses/:id`

**Request Body** (all fields optional):
```json
{
  "name": "New Name",
  "nameEn": "New English Name",
  "addressText": "New address text",
  "lat": 10.8510,              // Location change triggers re-calculation
  "lon": 106.7720,
  "segmentId": "new-segment",  // Manually override segment
  "zoneId": "new-zone",
  "wardName": "New Ward",
  "districtName": "New District",
  "addressType": "HOSPITAL"
}
```

**Response** (200):
```json
{
  "success": true,
  "message": "Address updated successfully",
  "data": {
    // ... updated address data with recalculated projection
  }
}
```

---

### 6. Delete Address

Delete an address permanently.

**Endpoint**: `DELETE /api/v1/addresses/:id`

**Response** (200):
```json
{
  "success": true,
  "message": "Address deleted successfully"
}
```

---

### 7. Batch Import Addresses

Efficiently import many addresses at once with automatic segment and zone association.

**Endpoint**: `POST /api/v1/addresses/batch`

**Request Body**:
```json
{
  "addresses": [
    {
      "name": "School 1",
      "lat": 10.8505,
      "lon": 106.7717,
      "addressType": "SCHOOL"
    },
    {
      "name": "Hospital 1",
      "lat": 10.8510,
      "lon": 106.7720,
      "addressType": "HOSPITAL"
    }
    // ... up to 1000 addresses
  ],
  "autoCalculateSegments": true,  // Auto-find nearest segments (default: true)
  "autoCalculateZones": true      // Auto-find containing zones (default: true)
}
```

**Response** (201 or 207):
```json
{
  "success": true,
  "message": "Batch import completed: 1998 successful, 2 failed",
  "data": {
    "total": 2000,
    "successful": 1998,
    "failed": 2,
    "errors": [
      {
        "index": 150,
        "name": "Invalid Address",
        "error": "Invalid coordinates"
      },
      {
        "index": 892,
        "name": "Duplicate Address",
        "error": "Address already exists"
      }
    ],
    "addresses": [
      // ... successfully created addresses
    ]
  }
}
```

**Status Codes**:
- `201`: All addresses imported successfully
- `207`: Partial success (some addresses failed)

**Limits**:
- Max batch size: 1000 addresses per request
- Processes in batches of 100 internally

---

### 8. Get Addresses by Segment

Get all addresses on a specific road segment.

**Endpoint**: `GET /api/v1/addresses/segments/:segmentId`

**Query Parameters**: Same as List Addresses (page, limit, search, etc.)

**Response** (200):
```json
{
  "success": true,
  "data": [
    // ... addresses on this segment, ordered by segment_position
  ],
  "paging": {
    "page": 1,
    "limit": 20,
    "total": 45,
    "totalPages": 3
  }
}
```

---

### 9. Get Addresses by Zone

Get all addresses within a specific zone.

**Endpoint**: `GET /api/v1/addresses/zones/:zoneId`

**Query Parameters**: Same as List Addresses (page, limit, search, etc.)

**Response** (200):
```json
{
  "success": true,
  "data": [
    // ... addresses in this zone
  ],
  "paging": {
    "page": 1,
    "limit": 20,
    "total": 234,
    "totalPages": 12
  }
}
```

---

## Data Model

### Address Structure

```typescript
interface Address {
  // Identity
  id: string                    // UUID

  // Basic Info
  name: string                  // Vietnamese name
  nameEn?: string               // English name (optional)
  addressText?: string          // Full address text (optional)

  // Location
  lat: number                   // Latitude (-90 to 90)
  lon: number                   // Longitude (-180 to 180)
  geohash?: string              // Geohash (precision 7 = ~76m)

  // Segment Association (parent curve)
  segmentId?: string            // Road segment ID
  segmentName?: string          // Denormalized segment name
  roadType?: string             // Denormalized road type
  segmentPosition?: number      // Position along segment (0.0 to 1.0)
  distanceToSegment?: number    // Perpendicular distance (meters)
  projectedLat?: number         // Projected point on segment
  projectedLon?: number

  // Zone Information
  zoneId?: string
  zoneName?: string             // Denormalized zone name
  wardName?: string
  districtName?: string

  // Type
  addressType: AddressType      // SCHOOL, HOSPITAL, etc.

  // Metadata
  createdAt: Date
  updatedAt: Date
}
```

---

## Database Schema

```sql
CREATE TABLE addresses (
  address_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  -- Basic info
  name TEXT NOT NULL,
  name_en TEXT,
  address_text TEXT,

  -- Location
  lat DOUBLE PRECISION NOT NULL,
  lon DOUBLE PRECISION NOT NULL,
  geohash TEXT,                             -- For fast proximity pre-filtering
  geometry geometry(Point,4326),            -- PostGIS point for exact distance

  -- Segment association
  segment_id UUID REFERENCES road_segments(segment_id),
  segment_position DOUBLE PRECISION,        -- 0.0 to 1.0
  distance_to_segment DOUBLE PRECISION,     -- Meters
  projected_lat DOUBLE PRECISION,
  projected_lon DOUBLE PRECISION,

  -- Zone info
  zone_id UUID REFERENCES zones(zone_id),
  ward_name TEXT,
  district_name TEXT,

  -- Type
  address_type address_type NOT NULL DEFAULT 'GENERAL',

  -- Metadata
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for fast queries
CREATE INDEX addresses_segment_id_idx ON addresses(segment_id);
CREATE INDEX addresses_zone_id_idx ON addresses(zone_id);
CREATE INDEX addresses_name_idx ON addresses(name);
CREATE INDEX addresses_geohash_idx ON addresses(geohash);
CREATE INDEX addresses_lat_lon_idx ON addresses(lat, lon);
CREATE INDEX addresses_geometry_idx ON addresses USING GIST(geometry);  -- PostGIS spatial index
```

---

## Performance Notes

### Nearest Address Query Performance

1. **Geohash Pre-filtering**:
   - Narrows search to ~9 cells (center + 8 neighbors)
   - Each cell covers ~76m × 76m area
   - Reduces candidate set by 90-99%

2. **PostGIS Distance Calculation**:
   - Uses GIST index for spatial queries
   - `ST_DWithin` with geography type for accuracy
   - Haversine distance calculation

3. **Typical Performance**:
   - 10,000 addresses: ~5-10ms
   - 100,000 addresses: ~10-20ms
   - 1,000,000 addresses: ~20-50ms

### Automatic Segment Association

- Uses PostGIS `ST_DWithin` to find candidates within 500m
- Projects address onto top 5 nearest segments
- Chooses segment with minimum perpendicular distance
- Typical time: ~5-15ms per address

---

## Use Cases

### 1. Delivery Address Lookup
```typescript
// Find addresses near delivery point
GET /api/v1/addresses/nearest?lat=10.8505&lon=106.7717&limit=10&maxDistance=500

// User selects address from list
```

### 2. Street View Navigation
```typescript
// Get all addresses on a street segment
GET /api/v1/addresses/segments/{segmentId}

// Addresses returned ordered by segment_position (0.0 to 1.0)
// Can show "next address is 50m ahead"
```

### 3. Zone Coverage Analysis
```typescript
// Get all addresses in a delivery zone
GET /api/v1/addresses/zones/{zoneId}

// Analyze coverage, density, etc.
```

### 4. POI Import
```typescript
// Import schools, hospitals, etc. from external data
POST /api/v1/addresses/batch
{
  "addresses": [...1000 addresses...],
  "autoCalculateSegments": true
}

// Automatically associates each address with nearest road
```

---

## Error Responses

**400 Bad Request**:
```json
{
  "success": false,
  "message": "Invalid coordinates"
}
```

**404 Not Found**:
```json
{
  "success": false,
  "message": "Address not found"
}
```

**500 Internal Server Error**:
```json
{
  "success": false,
  "message": "Failed to create address"
}
```
