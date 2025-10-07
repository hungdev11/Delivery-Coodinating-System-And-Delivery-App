# Center Routes

Base URL: `http://localhost:3003/api/v1`

## Endpoints

### GET /centers
- Description: Get all centers (paginated).
- Query:
```json
{
  "page": 0,
  "size": 10,
  "search": "optional",
  "code": "optional",
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
      "code": "CTR001",
      "name": "Center Name",
      "address": "string or null",
      "lat": 10.762622,
      "lon": 106.660172,
      "polygon": null
    }],
    "page": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1, "filters": [], "sorts": [], "selected": [] }
  }
}
```

### GET /centers/:id
- Description: Get center by ID.
- Response 200:
```json
{
  "result": {
    "id": "uuid",
    "code": "CTR001",
    "name": "Center Name",
    "address": null,
    "lat": null,
    "lon": null,
    "polygon": null
  }
}
```
- Response 404:
```json
{ "message": "Center not found" }
```

### GET /centers/code/:code
- Description: Get center by code.
- Response 200 same as above, 404 if not found.

### POST /centers
- Description: Create center.
- Body:
```json
{
  "code": "CTR001",
  "name": "Center Name",
  "address": "optional",
  "lat": 10.762622,
  "lon": 106.660172,
  "polygon": { "type": "Polygon", "coordinates": [] }
}
```
- Response 201:
```json
{
  "result": { "id": "uuid", "code": "CTR001", "name": "Center Name", "address": "...", "lat": 10.762622, "lon": 106.660172, "polygon": null },
  "message": "Center created successfully"
}
```
- Response 400:
```json
{ "message": "Center with this code already exists" }
```

### PUT /centers/:id
- Description: Update center.
- Body (all fields optional):
```json
{ "code": "CTR001", "name": "New Name", "address": "..", "lat": 0, "lon": 0, "polygon": null }
```
- Response 200:
```json
{ "result": { "id": "uuid", "code": "CTR001", "name": "New Name", "address": "..", "lat": 0, "lon": 0, "polygon": null }, "message": "Center updated successfully" }
```
- Errors: 404 not found, 400 code exists.

### DELETE /centers/:id
- Description: Delete center.
- Response 200:
```json
{ "result": null, "message": "Center deleted successfully" }
```
- Response 404:
```json
{ "message": "Center not found" }
```
