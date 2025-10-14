# Routing API Documentation

## Base URL
```
http://localhost:3000/api/v1/routing
```

## Endpoints

### 1. Calculate Route

Calculate optimal route between multiple waypoints with turn-by-turn navigation.

**Endpoint:** `POST /routing/route`

**Request Body:**
```json
{
  "waypoints": [
    { "lat": 10.8505, "lon": 106.7717 },
    { "lat": 10.8231, "lon": 106.6297 }
  ],
  "alternatives": false,
  "steps": true,
  "annotations": true
}
```

**Parameters:**
- `waypoints` (required): Array of coordinate objects
  - `lat` (number): Latitude
  - `lon` (number): Longitude
- `alternatives` (optional, boolean): Return alternative routes
- `steps` (optional, boolean): Include turn-by-turn steps (default: true)
- `annotations` (optional, boolean): Include traffic and POI data (default: true)

**Response:**
```json
{
  "success": true,
  "data": {
    "code": "Ok",
    "routes": [{
      "distance": 15420,
      "duration": 1250,
      "geometry": "{...}",
      "legs": [{
        "distance": 15420,
        "duration": 1250,
        "steps": [{
          "distance": 250,
          "duration": 45,
          "instruction": "Turn right onto Xa Lộ Hà Nội",
          "name": "Xa Lộ Hà Nội",
          "maneuver": {
            "type": "turn",
            "modifier": "right",
            "location": [106.7717, 10.8505]
          },
          "addresses": ["PTIT", "Học viện Công nghệ Bưu chính Viễn thông"],
          "trafficLevel": "NORMAL"
        }]
      }],
      "trafficSummary": {
        "averageSpeed": 35.5,
        "congestionLevel": "NORMAL",
        "estimatedDelay": 120
      }
    }]
  }
}
```

**Example:**
```bash
curl -X POST http://localhost:3000/api/v1/routing/route \
  -H "Content-Type: application/json" \
  -d '{
    "waypoints": [
      {"lat": 10.8505, "lon": 106.7717},
      {"lat": 10.8231, "lon": 106.6297}
    ]
  }'
```

---

### 2. Priority-Based Route (Delivery)

Calculate multi-stop route optimized by delivery priority. Higher priority stops are visited first.

**Endpoint:** `POST /routing/priority-route`

**Request Body:**
```json
{
  "waypoints": [
    { "lat": 10.8505, "lon": 106.7717 },
    { "lat": 10.8400, "lon": 106.7600 },
    { "lat": 10.8231, "lon": 106.6297 }
  ],
  "priorities": [3, 1]
}
```

**Parameters:**
- `waypoints` (required): Array of coordinate objects
- `priorities` (required): Array of priority values for each segment
  - Length must be `waypoints.length - 1`
  - Higher number = higher priority

**Use Case:**
- Delivery driver has 3 stops
- Stop 2 has priority 3 (urgent)
- Stop 3 has priority 1 (normal)
- System calculates: Start → Stop 2 (urgent) → Stop 3 (normal)

**Response:**
Same format as `/route` endpoint.

**Example:**
```bash
curl -X POST http://localhost:3000/api/v1/routing/priority-route \
  -H "Content-Type: application/json" \
  -d '{
    "waypoints": [
      {"lat": 10.8505, "lon": 106.7717},
      {"lat": 10.8400, "lon": 106.7600},
      {"lat": 10.8231, "lon": 106.6297}
    ],
    "priorities": [3, 1]
  }'
```

---

### 3. Simple Route (Quick Query)

Get route between two points using query parameters. Simplified version for quick lookups.

**Endpoint:** `GET /routing/simple`

**Query Parameters:**
- `fromLat` (required): Starting latitude
- `fromLon` (required): Starting longitude
- `toLat` (required): Destination latitude
- `toLon` (required): Destination longitude

**Example:**
```bash
curl "http://localhost:3000/api/v1/routing/simple?fromLat=10.8505&fromLon=106.7717&toLat=10.8231&toLon=106.6297"
```

**Response:**
Same format as POST `/route` endpoint.

---

### 4. Get OSRM Status

Check the status of OSRM instances (dual-instance setup for zero-downtime updates).

**Endpoint:** `GET /routing/status`

**Response:**
```json
{
  "success": true,
  "data": {
    "activeInstance": 1,
    "instance1Healthy": true,
    "instance2Healthy": true,
    "instance1Url": "http://osrm-instance-1:5000",
    "instance2Url": "http://osrm-instance-2:5000"
  }
}
```

**Response Fields:**
- `activeInstance` (number): Currently active instance (1 or 2)
- `instance1Healthy` (boolean): Health status of instance 1
- `instance2Healthy` (boolean): Health status of instance 2
- `instance1Url` (string): URL of instance 1
- `instance2Url` (string): URL of instance 2

**Example:**
```bash
curl "http://localhost:3000/api/v1/routing/status"
```

---

### 5. Switch OSRM Instance (Admin)

Switch the active OSRM instance. Used for zero-downtime updates when weights change.

**Endpoint:** `POST /routing/switch-instance`

**Request Body:**
```json
{
  "instance": 2
}
```

**Parameters:**
- `instance` (required): Target instance number (1 or 2)

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "Switched to instance 2"
  }
}
```

**Usage Pattern:**
1. Instance 1 is active, serving traffic
2. Rebuild instance 2 with updated weights
3. Verify instance 2 is healthy via `/status`
4. Call `/switch-instance` with `instance: 2`
5. Traffic now uses instance 2
6. Instance 1 becomes standby for next update

**Example:**
```bash
curl -X POST http://localhost:3000/api/v1/routing/switch-instance \
  -H "Content-Type: application/json" \
  -d '{"instance": 2}'
```

---

## Dual Instance Architecture

The system uses **two OSRM instances** for zero-downtime weight updates:

```
┌─────────────┐
│   Routing   │
│   Service   │
└──────┬──────┘
       │
  ┌────┴─────┐
  │  Router  │ (Selects active instance)
  └────┬─────┘
       │
  ┌────┴──────────────┐
  │                   │
┌─▼────────┐   ┌──────▼──┐
│ OSRM-1   │   │ OSRM-2  │
│ (Active) │   │(Standby)│
└──────────┘   └─────────┘
```

**Benefits:**
- Updates weights without service interruption
- Failover if one instance fails
- A/B testing of different routing profiles

**Weight Update Flow:**
1. Traffic data updated every 15-60 minutes
2. When significant weight changes detected:
   - Rebuild standby instance with new weights
   - Switch to standby instance
   - Old instance becomes new standby

---

## Response Fields

### Route Object
- `distance` (number): Total distance in meters
- `duration` (number): Total duration in seconds
- `geometry` (string): Encoded route geometry (GeoJSON)
- `legs` (array): Array of route legs
- `trafficSummary` (object): Traffic information

### Step Object
- `distance` (number): Step distance in meters
- `duration` (number): Step duration in seconds
- `instruction` (string): Human-readable instruction
- `name` (string): Road/street name
- `maneuver` (object): Turn maneuver details
- `addresses` (array): POIs along this step
- `trafficLevel` (string): Current traffic level

### Traffic Levels
- `FREE_FLOW`: Thông thoáng (green)
- `NORMAL`: Bình thường (yellow)
- `SLOW`: Chậm (orange)
- `CONGESTED`: Ùn tắc (red)
- `BLOCKED`: Tắc nghẽn hoàn toàn (dark red)

### Maneuver Types
- `turn`: Standard turn
- `depart`: Start of route
- `arrive`: End of route
- `merge`: Merge onto road
- `on-ramp`: Highway entrance
- `off-ramp`: Highway exit
- `fork`: Road splits
- `roundabout`: Roundabout
- `continue`: Continue straight

### Maneuver Modifiers
- `left`: Turn left
- `right`: Turn right
- `slight-left`: Bear left
- `slight-right`: Bear right
- `sharp-left`: Sharp left turn
- `sharp-right`: Sharp right turn
- `straight`: Go straight

---

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "At least 2 waypoints are required"
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Failed to calculate route",
  "error": "OSRM query failed"
}
```

### 503 Service Unavailable
```json
{
  "success": false,
  "message": "OSRM service unavailable"
}
```

---

## Integration Examples

### JavaScript/TypeScript
```typescript
const response = await fetch('http://localhost:3000/api/v1/routing/route', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    waypoints: [
      { lat: 10.8505, lon: 106.7717 },
      { lat: 10.8231, lon: 106.6297 }
    ]
  })
});

const data = await response.json();
if (data.success) {
  const route = data.data.routes[0];
  console.log(`Distance: ${route.distance}m`);
  console.log(`Duration: ${route.duration}s`);
}
```

### Python
```python
import requests

response = requests.post(
    'http://localhost:3000/api/v1/routing/route',
    json={
        'waypoints': [
            {'lat': 10.8505, 'lon': 106.7717},
            {'lat': 10.8231, 'lon': 106.6297}
        ]
    }
)

data = response.json()
if data['success']:
    route = data['data']['routes'][0]
    print(f"Distance: {route['distance']}m")
    print(f"Duration: {route['duration']}s")
```

### Java
```java
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);

Map<String, Object> request = new HashMap<>();
request.put("waypoints", Arrays.asList(
    Map.of("lat", 10.8505, "lon", 106.7717),
    Map.of("lat", 10.8231, "lon", 106.6297)
));

HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
ResponseEntity<String> response = restTemplate.postForEntity(
    "http://localhost:3000/api/v1/routing/route",
    entity,
    String.class
);
```

---

## Notes

1. **Coordinates:** Must be in WGS84 format (lat/lon)
2. **OSRM Required:** Routing endpoints require OSRM service to be running
3. **Rate Limiting:** Consider implementing rate limiting for production
4. **Caching:** Route responses can be cached for identical requests
5. **Traffic:** Traffic data updates every 15-60 minutes (configurable)

## See Also

- [ROUTING_SYSTEM.md](../../ROUTING_SYSTEM.md) - System architecture
- [NEXT_STEPS.md](../../NEXT_STEPS.md) - Setup guide
- [Zones API](./zones.md) - Zone management endpoints
