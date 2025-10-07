# Health Routes

## Overview
Health check endpoints for Settings Service monitoring.

**Base URL:** `http://localhost:21502`

---

## Endpoints

### GET /health
**Check service health status**

**Authentication:** Not required (Public)

**Response 200:**
```json
{
  "result": {
    "status": "UP",
    "service": "settings-service",
    "timestamp": "2025-10-08T00:06:00.554"
  },
  "message": "Settings Service is running"
}
```

**Example:**
```bash
curl http://localhost:21502/health
```

---

### GET /actuator/health
**Actuator health endpoint (standard Spring Boot)**

**Authentication:** Not required (Public)

**Response 200:**
```json
{
  "status": "UP",
  "details": {
    "service": "settings-service",
    "timestamp": "2025-10-08T00:06:00.554"
  }
}
```

**Example:**
```bash
curl http://localhost:21502/actuator/health
```

---

## Status Codes

| Status | Description |
|--------|-------------|
| `UP` | Service is healthy and running |
| `DOWN` | Service is not available |

---

## Integration

### From API Gateway
```bash
# Via proxy
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
```

### From Zone Service
```typescript
const response = await axios.get('http://localhost:21502/health');
if (response.data.result.status === 'UP') {
  console.log('Settings Service is healthy');
}
```

### From User Service
```java
String url = settingsServiceUrl + "/health";
ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
```
