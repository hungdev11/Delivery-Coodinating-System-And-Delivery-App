# Health Routes

Base URL: `http://localhost:8080/api/v1`

## Endpoints

### GET /health
- Description: Service health check (public).
- Response 200:
```json
{
  "result": {
    "status": "UP",
    "service": "api-gateway",
    "timestamp": "2024-01-01T00:00:00"
  },
  "message": "API Gateway is running"
}
```
