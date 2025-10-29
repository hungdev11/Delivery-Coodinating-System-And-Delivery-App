# Health Check API Documentation

**Service health monitoring endpoints**

---

## Base URL

```
http://localhost:21503
```

---

## Endpoints

### 1. Basic Health Check

Quick health status check.

**Endpoint:** `GET /health`

**Response:**

```json
{
  "status": "UP",
  "timestamp": "2025-01-15T10:30:00.000Z",
  "service": "zone-service",
  "version": "1.0.0"
}
```

**Status Codes:**
- `200 OK` - Service is healthy
- `503 Service Unavailable` - Service is down

**Example:**

```bash
curl http://localhost:21503/health
```

**Use Case:** Load balancer health checks, simple monitoring

---

### 2. Detailed Health Check

Comprehensive health status including dependencies.

**Endpoint:** `GET /health/detailed`

**Response (Healthy):**

```json
{
  "status": "UP",
  "timestamp": "2025-01-15T10:30:00.000Z",
  "service": "zone-service",
  "version": "1.0.0",
  "uptime": 3600,
  "dependencies": {
    "database": {
      "status": "UP",
      "responseTime": 5
    },
    "osrmInstance1": {
      "status": "UP",
      "url": "http://localhost:5000",
      "responseTime": 12
    },
    "osrmInstance2": {
      "status": "UP",
      "url": "http://localhost:5001",
      "responseTime": 15
    }
  }
}
```

**Response (Unhealthy):**

```json
{
  "status": "DOWN",
  "timestamp": "2025-01-15T10:30:00.000Z",
  "service": "zone-service",
  "version": "1.0.0",
  "uptime": 3600,
  "dependencies": {
    "database": {
      "status": "UP",
      "responseTime": 5
    },
    "osrmInstance1": {
      "status": "DOWN",
      "url": "http://localhost:5000",
      "error": "Connection refused"
    },
    "osrmInstance2": {
      "status": "UP",
      "url": "http://localhost:5001",
      "responseTime": 15
    }
  }
}
```

**Status Codes:**
- `200 OK` - All dependencies healthy
- `503 Service Unavailable` - One or more dependencies unhealthy

**Example:**

```bash
curl http://localhost:21503/health/detailed
```

**Use Case:** Detailed monitoring, troubleshooting, dashboards

---

### 3. Readiness Probe

Check if service is ready to accept traffic.

**Endpoint:** `GET /health/readiness`

**Response (Ready):**

```json
{
  "status": "READY",
  "timestamp": "2025-01-15T10:30:00.000Z"
}
```

**Response (Not Ready):**

```json
{
  "status": "NOT_READY",
  "timestamp": "2025-01-15T10:30:00.000Z",
  "reason": "Database not connected"
}
```

**Status Codes:**
- `200 OK` - Service ready for traffic
- `503 Service Unavailable` - Service not ready

**Example:**

```bash
curl http://localhost:21503/health/readiness
```

**Use Case:** Kubernetes readiness probes, load balancer routing decisions

**Checks:**
- ✅ Database connection established
- ✅ Prisma client initialized
- ✅ At least one OSRM instance healthy
- ✅ Express server listening

---

### 4. Liveness Probe

Check if service is alive (process running).

**Endpoint:** `GET /health/liveness`

**Response:**

```json
{
  "status": "ALIVE",
  "timestamp": "2025-01-15T10:30:00.000Z"
}
```

**Status Codes:**
- `200 OK` - Service is alive
- No response = service is dead (container should restart)

**Example:**

```bash
curl http://localhost:21503/health/liveness
```

**Use Case:** Kubernetes liveness probes, auto-restart on crash

**Note:** This endpoint always returns 200 if the process is running. It doesn't check dependencies.

---

## Response Fields

### Common Fields

| Field | Type | Description |
|-------|------|-------------|
| `status` | `string` | `UP`, `DOWN`, `READY`, `NOT_READY`, `ALIVE` |
| `timestamp` | `string` | ISO 8601 timestamp |
| `service` | `string` | Service name: `zone-service` |
| `version` | `string` | Service version from package.json |

### Detailed Health Fields

| Field | Type | Description |
|-------|------|-------------|
| `uptime` | `number` | Service uptime in seconds |
| `dependencies` | `object` | Status of each dependency |
| `dependencies.*.status` | `string` | `UP` or `DOWN` |
| `dependencies.*.responseTime` | `number` | Response time in milliseconds (if UP) |
| `dependencies.*.error` | `string` | Error message (if DOWN) |

---

## Integration Examples

### Shell Script Monitoring

```bash
#!/bin/bash
# monitor-zone-service.sh

check_health() {
  response=$(curl -s -w "%{http_code}" -o /tmp/health.json http://localhost:21503/health)
  http_code=${response: -3}

  if [ "$http_code" -eq 200 ]; then
    echo "✅ Zone Service is healthy"
    return 0
  else
    echo "❌ Zone Service is unhealthy (HTTP $http_code)"
    cat /tmp/health.json
    return 1
  fi
}

# Check every 30 seconds
while true; do
  check_health
  if [ $? -ne 0 ]; then
    # Send alert (email, Slack, etc.)
    echo "ALERT: Zone Service is down!"
  fi
  sleep 30
done
```

### Docker Compose Healthcheck

```yaml
services:
  zone-service:
    image: zone-service:latest
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:21503/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

### Kubernetes Probes

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: zone-service
spec:
  containers:
  - name: zone-service
    image: zone-service:latest
    ports:
    - containerPort: 21503

    # Liveness probe - restart if fails
    livenessProbe:
      httpGet:
        path: /health/liveness
        port: 21503
      initialDelaySeconds: 30
      periodSeconds: 10
      timeoutSeconds: 5
      failureThreshold: 3

    # Readiness probe - remove from service if fails
    readinessProbe:
      httpGet:
        path: /health/readiness
        port: 21503
      initialDelaySeconds: 10
      periodSeconds: 5
      timeoutSeconds: 3
      failureThreshold: 2
```

### Node.js Monitoring Client

```typescript
import axios from 'axios'

interface HealthStatus {
  status: string
  timestamp: string
  service: string
  version: string
  uptime?: number
  dependencies?: Record<string, any>
}

async function checkHealth(): Promise<HealthStatus> {
  try {
    const response = await axios.get<HealthStatus>(
      'http://localhost:21503/health/detailed',
      { timeout: 5000 }
    )

    return response.data
  } catch (error) {
    console.error('Health check failed:', error)
    throw error
  }
}

// Monitor every 30 seconds
setInterval(async () => {
  try {
    const health = await checkHealth()

    if (health.status === 'UP') {
      console.log('✅ Service healthy')
    } else {
      console.error('❌ Service unhealthy:', health)
      // Send alert
    }
  } catch (error) {
    console.error('❌ Service unreachable')
    // Send alert
  }
}, 30000)
```

### Python Monitoring

```python
import requests
import time

def check_health():
    try:
        response = requests.get(
            'http://localhost:21503/health/detailed',
            timeout=5
        )

        if response.status_code == 200:
            data = response.json()
            print(f"✅ Service healthy: {data['status']}")

            # Check dependencies
            for dep, status in data.get('dependencies', {}).items():
                if status['status'] != 'UP':
                    print(f"⚠️  {dep} is {status['status']}")

            return True
        else:
            print(f"❌ Service unhealthy: HTTP {response.status_code}")
            return False

    except requests.RequestException as e:
        print(f"❌ Service unreachable: {e}")
        return False

# Monitor loop
while True:
    check_health()
    time.sleep(30)
```

---

## Monitoring Best Practices

### 1. Use Appropriate Endpoints

| Scenario | Endpoint | Frequency |
|----------|----------|-----------|
| Load balancer routing | `/health/readiness` | Every 5-10s |
| Container restart | `/health/liveness` | Every 10-30s |
| Detailed monitoring | `/health/detailed` | Every 30-60s |
| Simple ping | `/health` | Every 10s |

### 2. Set Reasonable Timeouts

```typescript
// Good
const timeout = 5000  // 5 seconds

// Bad
const timeout = 30000  // 30 seconds (too long)
```

### 3. Implement Retry Logic

```typescript
async function checkHealthWithRetry(maxRetries = 3): Promise<boolean> {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const response = await axios.get('http://localhost:21503/health')
      if (response.status === 200) return true
    } catch (error) {
      console.warn(`Health check attempt ${i + 1} failed`)
    }

    await new Promise(resolve => setTimeout(resolve, 1000))
  }

  return false
}
```

### 4. Alert on Dependency Failures

```typescript
async function monitorDependencies() {
  const response = await axios.get('http://localhost:21503/health/detailed')
  const { dependencies } = response.data

  for (const [name, status] of Object.entries(dependencies)) {
    if (status.status !== 'UP') {
      sendAlert({
        service: 'zone-service',
        dependency: name,
        status: status.status,
        error: status.error
      })
    }
  }
}
```

---

## Troubleshooting

### Issue: `/health` returns 503

**Possible Causes:**
1. Database connection lost
2. Both OSRM instances down
3. Service starting up

**Solution:**

```bash
# Check detailed health
curl http://localhost:21503/health/detailed

# Check logs
docker logs zone-service --tail 50

# Check dependencies
docker ps | grep -E '(osrm|postgres)'
```

### Issue: `/health/readiness` returns NOT_READY

**Possible Causes:**
1. Database not connected
2. All OSRM instances unhealthy
3. Service initialization incomplete

**Solution:**

```bash
# Check readiness reason
curl http://localhost:21503/health/readiness

# Wait for startup (30-60 seconds)
# Then check again
```

### Issue: Health check timeout

**Possible Causes:**
1. Service overloaded
2. Database query slow
3. OSRM health check slow

**Solution:**

```bash
# Check service load
docker stats zone-service

# Check database
psql $DATABASE_URL -c "SELECT COUNT(*) FROM road_segments;"

# Check OSRM
curl http://localhost:5000/health
```

---

## See Also

- [Routing API](./routing.md) - Main routing endpoints
- [Zones API](./zones.md) - Zone management
- [Centers API](./centers.md) - Distribution centers

---

**Last Updated:** 2025-01-15
**Version:** 1.0.0
