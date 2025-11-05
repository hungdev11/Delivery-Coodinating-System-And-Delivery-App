# OSRM Data Management Routes

Base URL: `http://localhost:21503/api/v1`

## Endpoints

### POST /osrm/build/:instanceId
- Description: Build OSRM data for a specific instance (1 or 2).
- Params: `instanceId` (1 or 2)
- Response 200:
```json
{
  "result": {
    "success": true,
    "instanceId": 1,
    "message": "Build completed successfully"
  },
  "message": "OSRM data built successfully for instance 1"
}
```
- Response 400:
```json
{ "message": "Invalid instance ID. Must be 1 or 2" }
```

### POST /osrm/build-all
- Description: Build OSRM data for all instances.
- Response 200:
```json
{
  "result": [
    { "success": true, "instanceId": 1, "message": "Build completed" },
    { "success": true, "instanceId": 2, "message": "Build completed" }
  ],
  "message": "OSRM data build completed: 2/2 instances successful"
}
```

### POST /osrm/start/:instanceId
- Description: Start an OSRM instance.
- Params: `instanceId` (1 or 2)
- Response 200:
```json
{
  "result": { "instanceId": 1 },
  "message": "OSRM instance 1 started successfully"
}
```
- Response 400:
```json
{ "message": "Invalid instance ID. Must be 1 or 2" }
```

### POST /osrm/stop/:instanceId
- Description: Stop an OSRM instance.
- Params: `instanceId` (1 or 2)
- Response 200:
```json
{
  "result": { "instanceId": 1 },
  "message": "OSRM instance 1 stopped successfully"
}
```

### POST /osrm/rolling-restart
- Description: Perform rolling restart (switch active instance and restart the other).
- Response 200:
```json
{
  "result": {
    "previousActive": 1,
    "newActive": 2,
    "message": "Rolling restart completed"
  },
  "message": "Rolling restart completed successfully"
}
```

### GET /osrm/status
- Description: Get status of all OSRM instances.
- Response 200:
```json
{
  "result": {
    "instance1": {
      "running": true,
      "healthy": true,
      "port": 5000
    },
    "instance2": {
      "running": true,
      "healthy": true,
      "port": 5001
    },
    "activeInstance": 1
  }
}
```

### GET /osrm/status/:instanceId
- Description: Get status of a specific OSRM instance.
- Params: `instanceId` (1 or 2)
- Response 200:
```json
{
  "result": {
    "running": true,
    "healthy": true,
    "port": 5000,
    "instanceId": 1
  }
}
```

### GET /osrm/health
- Description: Health check for OSRM instances.
- Response 200:
```json
{
  "result": {
    "status": "healthy",
    "activeInstance": 1,
    "instances": {
      "instance1": { "healthy": true },
      "instance2": { "healthy": true }
    }
  }
}
```

### GET /osrm/validate/:instanceId
- Description: Validate OSRM data for a specific instance.
- Params: `instanceId` (1 or 2)
- Response 200:
```json
{
  "result": {
    "valid": true,
    "instanceId": 1,
    "message": "Data validation passed"
  }
}
```

### GET /osrm/history
- Description: Get build history for all instances.
- Response 200:
```json
{
  "result": [
    {
      "instanceId": 1,
      "buildTime": "2025-01-15T10:30:00Z",
      "status": "success",
      "duration": 120000
    }
  ]
}
```

### GET /osrm/history/:instanceId
- Description: Get build history for a specific instance.
- Params: `instanceId` (1 or 2)
- Response 200:
```json
{
  "result": [
    {
      "instanceId": 1,
      "buildTime": "2025-01-15T10:30:00Z",
      "status": "success",
      "duration": 120000
    }
  ]
}
```

### GET /osrm/deployment
- Description: Get deployment status of OSRM instances.
- Response 200:
```json
{
  "result": {
    "activeInstance": 1,
    "deploymentStatus": "ready",
    "instances": {
      "instance1": { "deployed": true, "ready": true },
      "instance2": { "deployed": true, "ready": true }
    }
  }
}
```
