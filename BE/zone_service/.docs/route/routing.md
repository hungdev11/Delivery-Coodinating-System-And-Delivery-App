# Routing API Documentation

**Complete routing API reference with examples and integration guides**

---

## Base URL

```
http://localhost:21503/api/v1/routing
```

---

## Table of Contents

1. [Calculate Route](#1-calculate-route)
2. [Multi-Stop Route](#2-multi-stop-route)
3. [Get OSRM Status](#3-get-osrm-status)
4. [Response Reference](#response-reference)
5. [Integration Examples](#integration-examples)
6. [Testing](#testing)

---

## 1. Calculate Route

Calculate optimal route between waypoints using OSRM with traffic-aware weights.

### Endpoint

```
POST /api/v1/routing/route
```

### Request Body

```json
{
  "waypoints": [
    { "lat": 10.8505, "lon": 106.7718 },
    { "lat": 10.8623, "lon": 106.8032 }
  ],
  "options": {
    "alternatives": false,
    "steps": true,
    "overview": "full"
  }
}
```

### Parameters

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `waypoints` | `Coordinate[]` | ✅ | Array of 2-25 waypoints |
| `waypoints[].lat` | `number` | ✅ | Latitude (10.7-11.0 for Thu Duc) |
| `waypoints[].lon` | `number` | ✅ | Longitude (106.6-106.9 for Thu Duc) |
| `options.alternatives` | `boolean` | ❌ | Return alternative routes (default: false) |
| `options.steps` | `boolean` | ❌ | Include turn-by-turn navigation (default: true) |
| `options.overview` | `string` | ❌ | Geometry detail: `full`, `simplified`, `false` (default: `full`) |

### Response (Success)

```json
{
  "code": "Ok",
  "routes": [{
    "distance": 6400.5,
    "duration": 510.2,
    "weight": 548.7,
    "geometry": {
      "type": "LineString",
      "coordinates": [[106.7718, 10.8505], [106.8032, 10.8623]]
    },
    "legs": [{
      "distance": 6400.5,
      "duration": 510.2,
      "steps": [{
        "distance": 250,
        "duration": 45,
        "instruction": "Head east on Phạm Văn Đồng",
        "name": "Phạm Văn Đồng",
        "maneuver": {
          "type": "depart",
          "location": [106.7718, 10.8505]
        }
      }, {
        "distance": 1200,
        "duration": 120,
        "instruction": "Turn right onto Xa Lộ Hà Nội",
        "name": "Xa Lộ Hà Nội",
        "maneuver": {
          "type": "turn",
          "modifier": "right",
          "location": [106.7750, 10.8510]
        }
      }]
    }]
  }],
  "waypoints": [
    { "location": [106.7718, 10.8505], "name": "Phạm Văn Đồng" },
    { "location": [106.8032, 10.8623], "name": "Man Thiện" }
  ]
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `code` | `string` | `Ok` or error code |
| `routes` | `Route[]` | Array of routes (1 or more if alternatives requested) |
| `routes[].distance` | `number` | Total distance in meters |
| `routes[].duration` | `number` | Total duration in seconds |
| `routes[].weight` | `number` | Total routing cost (includes traffic) |
| `routes[].geometry` | `object` | GeoJSON LineString geometry |
| `routes[].legs` | `Leg[]` | Route segments between waypoints |
| `waypoints` | `Waypoint[]` | Snapped waypoint locations |

### Error Responses

#### 400 Bad Request - Invalid Waypoints

```json
{
  "code": "InvalidInput",
  "message": "At least 2 waypoints are required"
}
```

#### 400 Bad Request - Out of Bounds

```json
{
  "code": "InvalidInput",
  "message": "Coordinates must be within Thu Duc area (lat: 10.7-11.0, lon: 106.6-106.9)"
}
```

#### 200 OK - No Route Found

```json
{
  "code": "NoRoute",
  "message": "No route found between waypoints"
}
```

**Note:** OSRM returns 200 with `code: "NoRoute"` when no route exists (not 404).

### Examples

#### Example 1: Simple Route

**Request:**

```bash
curl -X POST http://localhost:21503/api/v1/routing/route \
  -H "Content-Type: application/json" \
  -d '{
    "waypoints": [
      {"lat": 10.8505, "lon": 106.7718},
      {"lat": 10.8623, "lon": 106.8032}
    ]
  }'
```

**Response Summary:**
- Distance: 6.4 km
- Duration: 8.5 minutes
- Route: Phạm Văn Đồng → Xa Lộ Hà Nội → Man Thiện

#### Example 2: Route with Steps

```bash
curl -X POST http://localhost:21503/api/v1/routing/route \
  -H "Content-Type: application/json" \
  -d '{
    "waypoints": [
      {"lat": 10.8505, "lon": 106.7718},
      {"lat": 10.8623, "lon": 106.8032}
    ],
    "options": {
      "steps": true,
      "overview": "full"
    }
  }'
```

#### Example 3: Multiple Waypoints (Delivery Route)

```bash
curl -X POST http://localhost:21503/api/v1/routing/route \
  -H "Content-Type: application/json" \
  -d '{
    "waypoints": [
      {"lat": 10.8505, "lon": 106.7718},
      {"lat": 10.8550, "lon": 106.7800},
      {"lat": 10.8600, "lon": 106.7900},
      {"lat": 10.8623, "lon": 106.8032}
    ]
  }'
```

**Use Case:** Delivery driver with 3 stops

---

## 2. Multi-Stop Route

**COMING SOON** - Optimized multi-stop routing with priority support.

Calculate route with multiple delivery stops, optimized by priority or distance.

### Endpoint (Planned)

```
POST /api/v1/routing/multi-route
```

### Request Body (Planned)

```json
{
  "waypoints": [
    { "lat": 10.8505, "lon": 106.7718, "priority": 1 },
    { "lat": 10.8550, "lon": 106.7800, "priority": 3 },
    { "lat": 10.8600, "lon": 106.7900, "priority": 2 }
  ],
  "optimize": "priority"
}
```

### Parameters (Planned)

- `waypoints[].priority`: Delivery priority (1-10, higher = more urgent)
- `optimize`: `priority` (visit by urgency) or `distance` (shortest total distance)

---

## 3. Get OSRM Status

Check health and active instance of dual OSRM setup.

### Endpoint

```
GET /api/v1/routing/osrm-status
```

### Response

```json
{
  "activeInstance": 1,
  "instances": {
    "instance1": {
      "url": "http://localhost:5000",
      "healthy": true,
      "lastCheck": "2025-01-15T10:30:00.000Z"
    },
    "instance2": {
      "url": "http://localhost:5001",
      "healthy": true,
      "lastCheck": "2025-01-15T10:30:00.000Z"
    }
  },
  "totalRequests": 15234,
  "successRate": 99.8
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `activeInstance` | `number` | Currently active instance (1 or 2) |
| `instances.instance1.healthy` | `boolean` | Health status of instance 1 |
| `instances.instance2.healthy` | `boolean` | Health status of instance 2 |
| `totalRequests` | `number` | Total routing requests since startup |
| `successRate` | `number` | Success rate percentage |

### Example

```bash
curl http://localhost:21503/api/v1/routing/osrm-status
```

---

## Response Reference

### Route Object

```typescript
interface Route {
  distance: number        // Total distance (meters)
  duration: number        // Total duration (seconds)
  weight: number          // Routing cost (includes traffic weights)
  geometry: GeoJSON       // Route geometry
  legs: Leg[]             // Route legs between waypoints
}
```

### Leg Object

```typescript
interface Leg {
  distance: number        // Leg distance (meters)
  duration: number        // Leg duration (seconds)
  steps: Step[]           // Turn-by-turn steps
  summary: string         // Human-readable summary
}
```

### Step Object

```typescript
interface Step {
  distance: number              // Step distance (meters)
  duration: number              // Step duration (seconds)
  instruction: string           // Human-readable instruction
  name: string                  // Road/street name
  maneuver: Maneuver           // Turn maneuver details
  geometry: GeoJSON            // Step geometry
}
```

### Maneuver Object

```typescript
interface Maneuver {
  type: ManeuverType           // Type of maneuver
  modifier?: ManeuverModifier  // Turn modifier
  location: [number, number]   // [lon, lat] of maneuver
  bearing_before?: number      // Bearing before turn (degrees)
  bearing_after?: number       // Bearing after turn (degrees)
}
```

### Maneuver Types

| Type | Description | Example |
|------|-------------|---------|
| `turn` | Standard turn | Turn right onto Xa Lộ Hà Nội |
| `depart` | Start of route | Head east on Phạm Văn Đồng |
| `arrive` | End of route | You have arrived |
| `merge` | Merge onto road | Merge onto highway |
| `on ramp` | Highway entrance | Take ramp onto expressway |
| `off ramp` | Highway exit | Exit expressway |
| `fork` | Road splits | Keep left at fork |
| `roundabout` | Roundabout | Take 2nd exit at roundabout |
| `continue` | Continue straight | Continue on current road |

### Maneuver Modifiers

| Modifier | Description |
|----------|-------------|
| `left` | Turn left |
| `right` | Turn right |
| `slight left` | Bear left |
| `slight right` | Bear right |
| `sharp left` | Sharp left turn |
| `sharp right` | Sharp right turn |
| `straight` | Go straight |
| `uturn` | U-turn |

---

## Integration Examples

### JavaScript / TypeScript

```typescript
interface Waypoint {
  lat: number
  lon: number
}

interface RoutingResponse {
  code: string
  routes: Route[]
  waypoints: Waypoint[]
}

async function getRoute(
  waypoints: Waypoint[]
): Promise<RoutingResponse> {
  const response = await fetch(
    'http://localhost:21503/api/v1/routing/route',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ waypoints })
    }
  )

  if (!response.ok) {
    throw new Error(`Routing failed: ${response.statusText}`)
  }

  const data = await response.json()

  if (data.code !== 'Ok') {
    throw new Error(`No route found: ${data.message}`)
  }

  return data
}

// Usage
const waypoints = [
  { lat: 10.8505, lon: 106.7718 },
  { lat: 10.8623, lon: 106.8032 }
]

const result = await getRoute(waypoints)
const route = result.routes[0]

console.log(`Distance: ${(route.distance / 1000).toFixed(2)} km`)
console.log(`Duration: ${(route.duration / 60).toFixed(1)} min`)
console.log(`Steps: ${route.legs[0].steps.length}`)
```

### Python

```python
import requests
from typing import List, Dict

def get_route(waypoints: List[Dict[str, float]]) -> Dict:
    """
    Get route from Zone Service

    Args:
        waypoints: List of {"lat": float, "lon": float}

    Returns:
        Route data dict
    """
    url = 'http://localhost:21503/api/v1/routing/route'

    response = requests.post(url, json={'waypoints': waypoints})
    response.raise_for_status()

    data = response.json()

    if data['code'] != 'Ok':
        raise Exception(f"No route found: {data.get('message')}")

    return data

# Usage
waypoints = [
    {'lat': 10.8505, 'lon': 106.7718},
    {'lat': 10.8623, 'lon': 106.8032}
]

result = get_route(waypoints)
route = result['routes'][0]

print(f"Distance: {route['distance'] / 1000:.2f} km")
print(f"Duration: {route['duration'] / 60:.1f} min")
print(f"Steps: {len(route['legs'][0]['steps'])}")
```

### Java (Spring)

```java
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

public class ZoneServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl = "http://localhost:21503/api/v1/routing";

    public Map<String, Object> getRoute(List<Waypoint> waypoints) {
        String url = baseUrl + "/route";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> request = new HashMap<>();
        request.put("waypoints", waypoints);

        HttpEntity<Map<String, Object>> entity =
            new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            url, entity, Map.class
        );

        Map<String, Object> data = response.getBody();

        if (!"Ok".equals(data.get("code"))) {
            throw new RuntimeException("No route found");
        }

        return data;
    }

    static class Waypoint {
        public double lat;
        public double lon;

        public Waypoint(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }
}

// Usage
ZoneServiceClient client = new ZoneServiceClient();

List<ZoneServiceClient.Waypoint> waypoints = Arrays.asList(
    new ZoneServiceClient.Waypoint(10.8505, 106.7718),
    new ZoneServiceClient.Waypoint(10.8623, 106.8032)
);

Map<String, Object> result = client.getRoute(waypoints);
```

### Android (Kotlin)

```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class Waypoint(val lat: Double, val lon: Double)
data class RouteRequest(val waypoints: List<Waypoint>)
data class RouteResponse(
    val code: String,
    val routes: List<Route>,
    val waypoints: List<Waypoint>
)
data class Route(
    val distance: Double,
    val duration: Double,
    val legs: List<Leg>
)

interface ZoneServiceApi {
    @POST("api/v1/routing/route")
    suspend fun getRoute(@Body request: RouteRequest): RouteResponse
}

// Usage in ViewModel
class RoutingViewModel : ViewModel() {
    private val api = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:21503/")  // Android emulator localhost
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ZoneServiceApi::class.java)

    suspend fun getRoute(
        start: Waypoint,
        end: Waypoint
    ): Route? {
        return try {
            val response = api.getRoute(
                RouteRequest(waypoints = listOf(start, end))
            )

            if (response.code == "Ok") {
                response.routes.firstOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Routing", "Error getting route", e)
            null
        }
    }
}
```

---

## Testing

### Manual Testing

```bash
# Test simple route
curl -X POST http://localhost:21503/api/v1/routing/route \
  -H "Content-Type: application/json" \
  -d '{
    "waypoints": [
      {"lat": 10.8505, "lon": 106.7718},
      {"lat": 10.8623, "lon": 106.8032}
    ]
  }' | jq '.routes[0] | {distance, duration, steps: .legs[0].steps | length}'

# Expected output:
# {
#   "distance": 6400.5,
#   "duration": 510.2,
#   "steps": 11
# }
```

### Automated Testing

```bash
# Run OSRM stress test
cd /path/to/zone_service
npx tsx test-osrm-hard-routes.ts

# Expected: 100% success rate
```

### Performance Testing

```bash
# Test response time
time curl -X POST http://localhost:21503/api/v1/routing/route \
  -H "Content-Type: application/json" \
  -d '{
    "waypoints": [
      {"lat": 10.8505, "lon": 106.7718},
      {"lat": 10.8623, "lon": 106.8032}
    ]
  }' > /dev/null

# Target: < 100ms
```

---

## Common Issues

### Issue: "NoRoute" Response

**Cause:** No connected path between waypoints

**Solution:**

```bash
# 1. Check connectivity
npm run check:connectivity

# 2. Verify OSRM is healthy
docker ps | grep osrm

# 3. Try coordinates from database
npm run prisma:studio
# Browse road_nodes table, copy real coordinates
```

### Issue: Coordinates Outside Thu Duc

**Cause:** Waypoints outside Thu Duc boundary

**Thu Duc Bounds:**
- Latitude: 10.7 - 11.0
- Longitude: 106.6 - 106.9

**Solution:** Ensure coordinates are within bounds

### Issue: Timeout

**Cause:** OSRM instance not responding

**Solution:**

```bash
# Check OSRM logs
docker logs dss-osrm-1 --tail 50

# Restart if needed
docker-compose restart osrm-instance-1
```

---

## See Also

- [ARCHITECTURE.md](../ARCHITECTURE.md) - System architecture
- [OSRM.md](../OSRM.md) - OSRM setup and configuration
- [Zones API](./zones.md) - Zone management
- [Centers API](./centers.md) - Distribution centers

---

**Last Updated:** 2025-01-15
**Version:** 1.0.0
