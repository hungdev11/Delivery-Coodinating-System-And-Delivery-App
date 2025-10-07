# Health Routes

Base URL: `http://localhost:3003`

## Endpoints

### GET /health
- Description: Basic health check.
- Response:
```json
{
  "status": "UP",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "service": "zone-service"
}
```

### GET /health/detailed
- Description: Detailed health with dependencies.
- Response 200/503:
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

### GET /health/readiness
- Description: Readiness for traffic.
- Response 200/503:
```json
{ "status": "READY" }
```

### GET /health/liveness
- Description: Liveness probe.
- Response 200:
```json
{ "status": "ALIVE" }
```
