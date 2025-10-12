# Zone Routes

Base URL: `http://localhost:21503/api/v1`

## Endpoints

### GET /zones
- Description: Get all zones (paginated).
- Query:
```json
{
  "page": 0,
  "size": 10,
  "search": "optional",
  "code": "optional",
  "centerId": "optional",
  "filters": [],
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
    "page": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1, "filters": [], "sorts": [], "selected": [] }
  }
}
```

### GET /zones/:id
- Description: Get zone by ID.
- Response 200 similar to above, 404 if not found.

### GET /zones/code/:code
- Description: Get zone by code.
- Response 200 similar to above, 404 if not found.

### GET /zones/center/:centerId
- Description: Get zones by center ID.
- Response 200:
```json
{
  "result": [
    { "id": "uuid", "code": "ZONE001", "name": "Zone 1", "polygon": null, "centerId": "uuid", "centerCode": "CTR001", "centerName": "Center" }
  ]
}
```

### POST /zones
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
