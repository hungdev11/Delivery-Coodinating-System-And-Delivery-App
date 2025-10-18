# OSRM Self-Hosted Setup

This document describes the OSRM (Open Source Routing Machine) self-hosted setup with custom weight integration for the Zone Service.

## Overview

The system uses **dual OSRM instances** for zero-downtime updates when road network weights change due to traffic conditions or user feedback.

### Architecture

```
┌─────────────────┐
│  Zone Service   │
│  (Node.js/TS)   │
└────────┬────────┘
         │
    ┌────┴────┐
    │ Router  │ (Load balancer / Route selector)
    └────┬────┘
         │
    ┌────┴──────────────┐
    │                   │
┌───▼────────┐   ┌─────▼────────┐
│  OSRM-1    │   │   OSRM-2     │
│  (Active)  │   │  (Standby)   │
└────────────┘   └──────────────┘
```

### Dual Instance Strategy

1. **Instance 1** serves traffic while **Instance 2** is being rebuilt with updated weights
2. After rebuild completes, traffic switches to **Instance 2**
3. **Instance 1** can then be rebuilt with next update
4. This alternating pattern ensures zero downtime

## Components

### 1. OSRM Docker Instances

Two OSRM backend containers running:
- `osrm-instance-1` on port 5000
- `osrm-instance-2` on port 5001

### 2. OSRM Generator Service

Located at `src/services/osrm/osrm-generator.service.ts`

**Responsibilities:**
- Export road network from database to OSM XML format
- Generate custom Lua profile with dynamic weights
- Run OSRM processing tools (extract, partition, customize)
- Manage build lifecycle and deployment

### 3. OSRM Router Service

Located at `src/services/osrm/osrm-router.service.ts`

**Responsibilities:**
- Query active OSRM instance for routes
- Handle failover between instances
- Format responses for client applications

### 4. Traffic Integration

Located at `src/services/traffic/traffic-integration.service.ts`

**Responsibilities:**
- Fetch traffic data from tracking-asia
- Update segment weights in database
- Trigger OSRM rebuild when weights change significantly

## Data Flow

### Initial Setup

1. **Seed Database**
   ```bash
   npm run seed:zones  # Load district boundaries
   npm run seed:roads  # Load road network
   ```

2. **Generate Initial OSRM Data**
   ```bash
   npm run osrm:generate  # Creates OSM XML + Lua profile
   ```

3. **Start OSRM Instances**
   ```bash
   docker-compose up osrm-instance-1 osrm-instance-2
   ```

### Runtime Updates

1. **Traffic Update** (every 15-60 minutes)
   - TrafficIntegrationService fetches traffic data
   - Updates `traffic_conditions` table
   - Recalculates segment weights (`delta_weight`, `current_weight`)
   - Logs changes to `weight_history`

2. **OSRM Rebuild** (when significant weight changes occur)
   - OSRMGeneratorService exports updated network to OSM XML
   - Generates Lua profile with new weights
   - Runs OSRM processing on standby instance
   - Switches active instance after successful build

## Custom Weight System

### Weight Calculation

Each road segment has three weight values:

```typescript
base_weight = f(length, speed, road_type, lanes)
delta_weight = traffic_impact + user_feedback_adjustment
current_weight = base_weight + delta_weight
```

### Base Weight

Static weight based on road characteristics:
- **Time component**: `length / avg_speed`
- **Road type multiplier**: motorway (1.0) → path (3.0)
- **Lane multiplier**: more lanes = lower weight

### Delta Weight

Dynamic adjustment from:
- **Traffic conditions**: multiplier from tracking-asia (0.9 - 3.0×)
- **User feedback**: adjustments from approved user reports

### Lua Profile Integration

The custom Lua profile (`custom_car.lua`) reads the `custom_weight` tag from OSM XML:

```lua
function process_way(profile, way, result, relations)
  local custom_weight = tonumber(way:get_value_by_key("custom_weight"))

  if custom_weight and custom_weight > 0 then
    result.forward_rate = 60.0 / custom_weight
    result.backward_rate = 60.0 / custom_weight
    result.weight = custom_weight
  end
end
```

## OSRM Data Directory Structure

```
osrm_data/
├── osrm-instance-1/
│   ├── network.osm.xml          # OSM export from database
│   ├── custom_car.lua           # Custom weight Lua profile
│   ├── network.osrm             # OSRM graph (from extract)
│   ├── network.osrm.edges       # Edge data (from partition)
│   ├── network.osrm.cells       # Cell data (from partition)
│   ├── network.osrm.mldgr       # Multi-level graph (from customize)
│   └── network.osrm.partition   # Partition data
└── osrm-instance-2/
    └── (same structure)
```

## Docker Compose Configuration

### OSRM Instance 1
```yaml
osrm-instance-1:
  image: osrm/osrm-backend:latest
  container_name: dss-osrm-1
  volumes:
    - ./BE/zone_service/osrm_data/osrm-instance-1:/data
  command: osrm-routed --algorithm mld /data/network.osrm
  ports:
    - "5000:5000"
```

### OSRM Instance 2
```yaml
osrm-instance-2:
  image: osrm/osrm-backend:latest
  container_name: dss-osrm-2
  volumes:
    - ./BE/zone_service/osrm_data/osrm-instance-2:/data
  command: osrm-routed --algorithm mld /data/network.osrm
  ports:
    - "5001:5000"
```

## API Endpoints

### Zone Service Routing API

#### Get Route
```
POST /api/v1/routing/route
```

**Request:**
```json
{
  "waypoints": [
    {"lat": 10.8505, "lon": 106.7718},
    {"lat": 10.8623, "lon": 106.8032}
  ],
  "options": {
    "alternatives": true,
    "steps": true,
    "overview": "full"
  }
}
```

**Response:**
```json
{
  "code": "Ok",
  "routes": [{
    "distance": 3421.5,
    "duration": 512.3,
    "weight": 548.7,
    "geometry": "...",
    "legs": [...],
    "steps": [...]
  }]
}
```

#### Get Multi-Stop Route
```
POST /api/v1/routing/multi-route
```

For delivery routes with multiple stops and priorities.

## Operational Procedures

### Build New OSRM Instance

```bash
# Generate OSRM data for next instance
npm run osrm:generate

# Check build status
npm run osrm:status

# Deploy when ready
npm run osrm:deploy
```

### Monitor Active Instance

```bash
# Check which instance is active
curl http://localhost:21503/api/v1/routing/status

# Health check OSRM directly
curl http://localhost:5000/health  # Instance 1
curl http://localhost:5001/health  # Instance 2
```

### Force Instance Switch

```bash
# Switch to specific instance (emergency only)
npm run osrm:switch -- --instance 2
```

## Troubleshooting

### Issue: OSRM returns "No route found"

**Possible causes:**
1. Road network incomplete in database
2. OSRM data not built from latest database
3. Source/destination outside coverage area (Thu Duc)

**Resolution:**
```bash
# Verify data in database
npm run prisma:studio

# Rebuild OSRM data
npm run osrm:generate
```

### Issue: Instance won't start

**Check logs:**
```bash
docker logs dss-osrm-1
docker logs dss-osrm-2
```

**Common problems:**
- `.osrm` files missing (run osrm-extract)
- Corrupted data files (rebuild from database)
- Port already in use (check `docker ps`)

### Issue: Routing quality poor

**Possible causes:**
1. Weights not reflecting traffic conditions
2. Traffic data not updating
3. Lua profile not applied correctly

**Resolution:**
```bash
# Check traffic updates
npm run test:traffic

# Verify weight calculations
npm run test:weights

# Check OSRM build logs
docker logs dss-osrm-1 | grep -i weight
```

## Performance Considerations

### OSRM Processing Time

- **Extract**: ~5-15 seconds for Thu Duc area
- **Partition**: ~10-30 seconds
- **Customize**: ~5-10 seconds
- **Total rebuild**: ~30-60 seconds

### Traffic Update Frequency

Recommended: **30 minutes**
- Too frequent: unnecessary OSRM rebuilds
- Too infrequent: outdated traffic information

### Rebuild Triggers

Rebuild OSRM when:
1. Average weight change > 10%
2. Critical roads have > 20% weight change
3. Manual rebuild requested
4. Scheduled rebuild (e.g., daily)

## References

- [OSRM Documentation](http://project-osrm.org/)
- [OSRM HTTP API](https://github.com/Project-OSRM/osrm-backend/blob/master/docs/http.md)
- [OSRM Lua Profiles](https://github.com/Project-OSRM/osrm-backend/blob/master/docs/profiles.md)
- [tracking-asia API](https://tracking-asia.com/docs) (if available)
