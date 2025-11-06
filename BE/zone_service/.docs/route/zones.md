# Zone Routes

## API Versions

- **V0**: Simple paging and sorting (no dynamic filters) - `http://localhost:21503/api/v0/zones`
- **V1**: Dynamic filtering with group-level operations - `http://localhost:21503/api/v1/zones`
- **V2**: Enhanced filtering with pair-level operations - `http://localhost:21503/api/v2/zones`

## V0 Endpoints (Simple Paging)

### POST /v0/zones
- Description: Get all zones with simple paging and sorting (no dynamic filters).
- Body:
```json
{
  "page": 0,
  "size": 10,
  "sorts": [
    {
      "field": "name",
      "direction": "asc"
    }
  ],
  "search": "optional",
  "selected": []
}
```
- Response 200:
```json
{
  "result": {
    "data": [{
      "id": "uuid",
      "code": "ZONE001",
      "name": "Zone Name",
      "polygon": null,
      "centerId": "uuid",
      "centerCode": "CTR001",
      "centerName": "Center"
    }],
    "page": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1, "filters": null, "sorts": [...] }
  }
}
```

## V1 Endpoints (Dynamic Filtering - Group Level)

### POST /v1/zones
- Description: Get all zones (paginated with advanced filtering/sorting).
- Body:
```json
{
  "page": 0,
  "size": 10,
  "search": "optional",
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "code",
        "operator": "contains",
        "value": "ZONE"
      }
    ]
  },
  "sorts": [],
  "selected": []
}
```
- Response 200:
```json
{
  "result": {
    "data": [{
      "id": "uuid",
      "code": "ZONE001",
      "name": "Zone Name",
      "polygon": null,
      "centerId": "uuid",
      "centerCode": "CTR001",
      "centerName": "Center"
    }],
    "page": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1, "filters": {...}, "sorts": [], "selected": [] }
  }
}
```

## V2 Endpoints (Enhanced Filtering - Pair Level)

### POST /v2/zones
- Description: Get all zones with enhanced filtering (operations between each pair).
- Body:
```json
{
  "page": 0,
  "size": 10,
  "search": "optional",
  "filters": {
    "type": "group",
    "items": [
      {
        "type": "condition",
        "field": "code",
        "operator": "CONTAINS",
        "value": "ZONE"
      },
      {
        "type": "operator",
        "value": "AND"
      },
      {
        "type": "condition",
        "field": "centerId",
        "operator": "IS_NOT_NULL"
      }
    ]
  },
  "sorts": [],
  "selected": []
}
```
- Response 200: Same as V1

## Common Endpoints (All Versions)

### GET /zones/:id
- Description: Get zone by ID.
- Response 200 similar to above, 404 if not found.

### GET /zones/filterable-fields
- Description: Get list of filterable fields for zones.
- Response 200:
```json
{
  "result": ["code", "name", "centerId", ...]
}
```

### GET /zones/sortable-fields
- Description: Get list of sortable fields for zones.
- Response 200:
```json
{
  "result": ["code", "name", "centerId", ...]
}
```

### POST /zones/create
- Description: Create zone.
- Body:
```json
{ "code": "ZONE001", "name": "Zone Name", "polygon": null, "centerId": "uuid" }
```
- Response 201:
```json
{ "result": { "id": "uuid", "code": "ZONE001", "name": "Zone Name", "polygon": null, "centerId": "uuid", "centerCode": "CTR001", "centerName": "Center" }, "message": "Zone created successfully" }
```
- Errors: 400 code exists, 404 center not found.

### PUT /zones/:id
- Description: Update zone.
- Body (optional fields):
```json
{ "code": "ZONE001", "name": "New Zone", "polygon": null, "centerId": "uuid" }
```
- Response 200:
```json
{ "result": { "id": "uuid", "code": "ZONE001", "name": "New Zone", "polygon": null, "centerId": "uuid", "centerCode": "CTR001", "centerName": "Center" }, "message": "Zone updated successfully" }
```
- Errors: 404 zone/center not found, 400 code exists.

### DELETE /zones/:id
- Description: Delete zone.
- Response 200:
```json
{ "result": null, "message": "Zone deleted successfully" }
```
- Response 404:
```json
{ "message": "Zone not found" }
```
