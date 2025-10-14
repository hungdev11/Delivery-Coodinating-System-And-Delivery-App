# Settings Proxy Routes

## Overview
API Gateway proxy endpoints for Settings Service. All requests are forwarded to the Settings Service.

Settings are identified by **group/key pair**:
- **Group**: Service/module identifier (e.g., "zone-service", "user-service")
- **Key**: Specific setting name

All responses follow the RESTful standard using `BaseResponse<T>`.

**Base URL:** `http://localhost:8080/api/v1/settings`

---

## Endpoints

### GET /{group}
**Get all settings by group (service identifier)**

Proxies to: `GET /api/v1/settings/{group}` on Settings Service

**Parameters:**
- `group` (path) - Service/module identifier

**Authentication:** Required

**Response 200:**
```json
{
  "result": [
    {
      "key": "max_upload_size",
      "group": "file_config",
      "description": "Maximum file upload size in MB",
      "type": "INTEGER",
      "value": "100",
      "level": 0,
      "isReadOnly": false,
      "displayMode": "NUMBER",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-02T10:30:00",
      "updatedBy": "admin"
    }
  ],
  "message": null
}
```

---

### GET /{group}/{key}
**Get setting by group and key pair**

Proxies to: `GET /api/v1/settings/{group}/{key}` on Settings Service

**Parameters:**
- `group` (path) - Service/module identifier
- `key` (path) - Setting key

**Authentication:** Required

**Response 200:**
```json
{
  "result": {
    "key": "max_upload_size",
    "group": "file_config",
    "description": "Maximum file upload size in MB",
    "type": "INTEGER",
    "value": "100",
    "level": 0,
    "isReadOnly": false,
    "displayMode": "NUMBER",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-02T10:30:00",
    "updatedBy": "admin"
  },
  "message": null
}
```

---

### GET /{group}/{key}/value
**Get setting value only**

Proxies to: `GET /api/v1/settings/{group}/{key}/value` on Settings Service

**Parameters:**
- `group` (path) - Service/module identifier
- `key` (path) - Setting key

**Authentication:** Required

**Response 200:**
```json
{
  "result": "100",
  "message": null
}
```

---

### PUT /{group}/{key}
**Upsert (create or update) a setting**

Proxies to: `PUT /api/v1/settings/{group}/{key}` on Settings Service

**Parameters:**
- `group` (path) - Service/module identifier
- `key` (path) - Setting key

**Headers:**
- `X-User-Id` (optional) - User identifier for audit trail

**Authentication:** Required (Admin role recommended)

**Request Body:**
```json
{
  "key": "max_upload_size",
  "group": "file_config",
  "description": "Maximum file upload size in MB",
  "type": "INTEGER",
  "value": "100",
  "level": 0,
  "isReadOnly": false,
  "displayMode": "NUMBER"
}
```

**Response 200:**
```json
{
  "result": {
    "key": "max_upload_size",
    "group": "file_config",
    "description": "Maximum file upload size in MB",
    "type": "INTEGER",
    "value": "100",
    "level": 0,
    "isReadOnly": false,
    "displayMode": "NUMBER",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-02T10:30:00",
    "updatedBy": "admin"
  },
  "message": "Setting saved successfully"
}
```

---

### DELETE /{group}/{key}
**Delete a setting**

Proxies to: `DELETE /api/v1/settings/{group}/{key}` on Settings Service

**Parameters:**
- `group` (path) - Service/module identifier
- `key` (path) - Setting key

**Authentication:** Required (Admin role recommended)

**Response 204:** No Content

---

## Examples

### Get zone-service settings
```bash
curl -X GET http://localhost:8080/api/v1/settings/zone-service \
  -H "Authorization: Bearer {token}"
```

### Get specific setting
```bash
curl -X GET http://localhost:8080/api/v1/settings/zone-service/default_zone_radius \
  -H "Authorization: Bearer {token}"
```

### Get setting value only
```bash
curl -X GET http://localhost:8080/api/v1/settings/zone-service/default_zone_radius/value \
  -H "Authorization: Bearer {token}"
```

### Create or update a setting
```bash
curl -X PUT http://localhost:8080/api/v1/settings/zone-service/default_zone_radius \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin-123" \
  -d '{
    "key": "default_zone_radius",
    "group": "zone-service",
    "description": "Default radius for zones in meters",
    "type": "INTEGER",
    "value": "1000",
    "level": 0,
    "isReadOnly": false,
    "displayMode": "NUMBER"
  }'
```

### Delete a setting
```bash
curl -X DELETE http://localhost:8080/api/v1/settings/zone-service/default_zone_radius \
  -H "Authorization: Bearer {token}"
```

---

## Authentication & Authorization

All endpoints require authentication via the API Gateway:
- **Read operations** (GET): All authenticated users
- **Write operations** (PUT, DELETE): Admin role recommended

The API Gateway validates JWT tokens before proxying requests to the Settings Service.

---

## Error Responses

### 401 Unauthorized
```json
{
  "message": "Unauthorized - Invalid or missing token",
  "result": null
}
```

### 403 Forbidden
```json
{
  "message": "Forbidden - Insufficient permissions",
  "result": null
}
```

### 404 Not Found
```json
{
  "message": "Setting not found",
  "result": null
}
```

### 500 Internal Server Error
```json
{
  "message": "Internal server error",
  "result": null
}
```
