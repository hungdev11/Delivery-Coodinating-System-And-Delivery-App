# Settings Routes

## Overview
API endpoints for managing system settings. All settings are identified by **group/key pair**.

- **Group**: Identifies the service/module (e.g., "zone-service", "user-service", "payment-config")
- **Key**: Specific setting name within the group
- All responses follow the RESTful standard defined in `RESTFUL.md` using `BaseResponse<T>`

**Base URL:** `http://localhost:8082/api/v1/settings`

## Setting Level
Settings use **numeric level (ORDINAL)** for priority:
- `0` - SYSTEM (highest priority)
- `1` - APPLICATION
- `2` - SERVICE
- `3` - FEATURE
- `4` - USER (lowest priority)

## Setting Types
- `STRING` - String value
- `INTEGER` - Integer value
- `DECIMAL` - Decimal number
- `BOOLEAN` - Boolean value (true/false)
- `JSON` - JSON object value

## Display Modes
- `TEXT` - Plain text input
- `PASSWORD` - Masked password field
- `CODE` - Code editor (JSON, XML, etc.)
- `NUMBER` - Number input
- `TOGGLE` - Boolean toggle
- `TEXTAREA` - Multi-line text
- `URL` - URL/Link
- `EMAIL` - Email address

---

## Endpoints

### GET /{group}
**Get all settings by group (service identifier)**

Retrieves all settings for a specific service/module.

**Parameters:**
- `group` (path) - Service/module identifier (e.g., "zone-service")

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

Retrieves a specific setting by its group and key.

**Parameters:**
- `group` (path) - Service/module identifier
- `key` (path) - Setting key

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

**Errors:**
- 404 - Setting not found

---

### GET /{group}/{key}/value
**Get setting value only by group and key pair**

Retrieves only the value of a specific setting.

**Parameters:**
- `group` (path) - Service/module identifier
- `key` (path) - Setting key

**Response 200:**
```json
{
  "result": "100",
  "message": null
}
```

**Errors:**
- 404 - Setting not found

---

### PUT /{group}/{key}
**Upsert (create or update) a setting by group/key pair**

Creates a new setting or updates an existing one. This is the **main API** for settings modification.

**Parameters:**
- `group` (path) - Service/module identifier
- `key` (path) - Setting key

**Headers:**
- `X-User-Id` (optional) - User identifier for audit trail (default: "system")

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

**Response 200 (Create or Update):**
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

**Errors:**
- 400 - Validation failed
- 403 - Read-only setting cannot be modified

---

### DELETE /{group}/{key}
**Delete a setting by group/key pair**

Deletes a specific setting.

**Parameters:**
- `group` (path) - Service/module identifier
- `key` (path) - Setting key

**Response 200:**
```json
{
  "result": null,
  "message": "Setting deleted successfully"
}
```

**Errors:**
- 404 - Setting not found
- 403 - Read-only setting cannot be deleted

---

## Examples

### Example 1: Get zone-service settings
```bash
GET /api/v1/settings/zone-service
```

### Example 2: Get specific setting
```bash
GET /api/v1/settings/zone-service/default_zone_radius
```

### Example 3: Get setting value only
```bash
GET /api/v1/settings/zone-service/default_zone_radius/value
```

### Example 4: Create or update a setting
```bash
PUT /api/v1/settings/zone-service/default_zone_radius
Headers: X-User-Id: admin-123

{
  "key": "default_zone_radius",
  "group": "zone-service",
  "description": "Default radius for zones in meters",
  "type": "INTEGER",
  "value": "1000",
  "level": 0,
  "isReadOnly": false,
  "displayMode": "NUMBER"
}
```

### Example 5: Delete a setting
```bash
DELETE /api/v1/settings/zone-service/default_zone_radius
```

---

## Best Practices

### Group Naming
Groups should clearly identify the service or module:
- Use kebab-case: `zone-service`, `user-service`, `payment-config`
- Be specific: `email-notification` instead of `notification`
- Include purpose: `feature-flags`, `cache-config`, `api-limits`

### Key Naming
Keys should be descriptive and follow snake_case:
- `max_upload_size`
- `default_zone_radius`
- `enable_email_notification`

### Level Selection
Choose appropriate level based on scope:
- **SYSTEM (0)**: Core system configurations
- **APPLICATION (1)**: Application-wide settings
- **SERVICE (2)**: Service-specific configurations
- **FEATURE (3)**: Feature toggles and flags
- **USER (4)**: User preferences

### Read-Only Settings
Mark critical settings as read-only (`isReadOnly: true`) to prevent accidental modifications via API.
