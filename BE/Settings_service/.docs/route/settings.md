# Settings Routes

Base URL: `http://localhost:<port>/api/v1/settings`

## Endpoints

### POST /settings
- Description: List settings with filtering/sorting/paging.
- Body:
```json
{
  "page": 0,
  "size": 10,
  "search": "optional",
  "filters": [],
  "sorts": [],
  "selected": []
}
```
- Response 200:
```json
{
  "status": "success",
  "data": {
    "data": [{
      "id": 1,
      "settingGroup": "user-service",
      "settingKey": "max-login-attempts",
      "settingValue": "5",
      "description": "Maximum login attempts",
      "type": "INTEGER",
      "isEditable": true
    }],
    "page": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
  }
}
```

### GET /settings/:group
- Description: Get all settings by group.
- Params: `group` (e.g., "user-service", "global")
- Response 200:
```json
{
  "status": "success",
  "data": [
    {
      "id": 1,
      "settingGroup": "user-service",
      "settingKey": "max-login-attempts",
      "settingValue": "5",
      "description": "Maximum login attempts",
      "type": "INTEGER",
      "isEditable": true
    }
  ]
}
```

### GET /settings/:group/:key
- Description: Get setting by group and key.
- Params: `group`, `key`
- Response 200:
```json
{
  "status": "success",
  "data": {
    "id": 1,
    "settingGroup": "user-service",
    "settingKey": "max-login-attempts",
    "settingValue": "5",
    "description": "Maximum login attempts",
    "type": "INTEGER",
    "isEditable": true
  }
}
```

### GET /settings/:group/:key/value
- Description: Get setting value only.
- Params: `group`, `key`
- Response 200:
```json
{
  "status": "success",
  "data": "5"
}
```

### PUT /settings/:group/:key
- Description: Upsert (create or update) a setting.
- Params: `group`, `key`
- Headers: `X-User-Id` (optional, default: "system")
- Body:
```json
{
  "settingValue": "10",
  "description": "Updated maximum login attempts",
  "type": "INTEGER",
  "isEditable": true
}
```
- Response 200:
```json
{
  "status": "success",
  "message": "Setting saved successfully",
  "data": {
    "id": 1,
    "settingGroup": "user-service",
    "settingKey": "max-login-attempts",
    "settingValue": "10",
    "description": "Updated maximum login attempts",
    "type": "INTEGER",
    "isEditable": true
  }
}
```

### DELETE /settings/:group/:key
- Description: Delete a setting.
- Params: `group`, `key`
- Response 200:
```json
{
  "status": "success",
  "message": "Setting deleted successfully"
}
```
