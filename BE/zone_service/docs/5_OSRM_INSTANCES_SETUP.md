# 5 OSRM Instances - Routing Modes Setup

## Overview

The system now generates 5 separate OSRM instances, each optimized for different routing strategies. All instances use **motorbike routing** with Vietnam-specific optimizations.

## Instances

### 1. Priority-First (`osrm-priority-first`)
- **Port**: 5000
- **Mode**: `priority_first`
- **Behavior**: 
  - Always visits high-priority waypoints
  - Accepts longer detours to serve priority stops
  - Lambda (λ) = 1.0 (high priority weight)
  - All `knp:*` overrides applied
- **Use Case**: Express deliveries where priority waypoints MUST be served first

### 2. Speed-Leaning (`osrm-speed-leaning`)
- **Port**: 5001
- **Mode**: `speed_leaning`
- **Behavior**: 
  - Prioritizes fast routes
  - Skips low-priority waypoints if detour cost is high
  - Lambda (λ) = 0.2 (low priority weight, high speed focus)
  - All `knp:*` overrides applied
- **Use Case**: Time-sensitive deliveries where speed matters most

### 3. Balanced (`osrm-balanced`)
- **Port**: 5002
- **Mode**: `balanced`
- **Behavior**: 
  - Balances priority and speed
  - Moderate detour acceptance for priority waypoints
  - Lambda (λ) = 0.5 (balanced)
  - All `knp:*` overrides applied
- **Use Case**: Standard deliveries with mixed priorities

### 4. No-Recommend (`osrm-no-recommend`)
- **Port**: 5003
- **Mode**: `no_recommend`
- **Behavior**: 
  - Ignores AI recommendations (`knp:point_score`, `knp:delta`)
  - Only applies block levels (`knp:block_level`)
  - Forces `knp:recommend_enabled=0`
- **Use Case**: Manual control, when AI suggestions are not trusted

### 5. Base (`osrm-base`)
- **Port**: 5004
- **Mode**: `base`
- **Behavior**: 
  - Pure base routing
  - No overrides applied (`knp:*` tags ignored)
  - Only uses base weights and shipper feedback
- **Use Case**: Baseline comparison, testing without overrides

## Architecture Components

### 1. Data Generation (`generate-osrm-data.ts`)
```typescript
const instances = [
  { name: 'osrm-priority-first', vehicle: 'motorbike', mode: 'priority_first' },
  { name: 'osrm-speed-leaning', vehicle: 'motorbike', mode: 'speed_leaning' },
  { name: 'osrm-balanced', vehicle: 'motorbike', mode: 'balanced' },
  { name: 'osrm-no-recommend', vehicle: 'motorbike', mode: 'no_recommend' },
  { name: 'osrm-base', vehicle: 'motorbike', mode: 'base' },
];
```

### 2. Override Application Logic

#### In `exportToOSMXML(instanceName, mode)`:
- Fetches `road_overrides` from database
- Applies overrides based on mode:
  - **base**: No overrides, adds `knp:mode=base` tag
  - **no_recommend**: Applies blocks, disables delta/point_score
  - **priority_first/speed_leaning/balanced**: Full overrides applied

#### In `generateMotorbikeLuaScript(mode)`:
- Reads `knp:*` tags from OSM XML
- Applies hard blocking (`knp:block_level=hard`)
- Calculates adjusted weight based on:
  - Shipper feedback
  - `knp:point_score` (if recommend enabled)
  - `knp:delta` (additive adjustment)
  - `knp:block_level` (soft/min penalties)

### 3. Docker Compose Services

```yaml
services:
  osrm-priority-first:
    ports: ["5000:5000"]
    volumes: ["./BE/zone_service/osrm_data/osrm-priority-first:/data:ro"]
  
  osrm-speed-leaning:
    ports: ["5001:5000"]
    volumes: ["./BE/zone_service/osrm_data/osrm-speed-leaning:/data:ro"]
  
  osrm-balanced:
    ports: ["5002:5000"]
    volumes: ["./BE/zone_service/osrm_data/osrm-balanced:/data:ro"]
  
  osrm-no-recommend:
    ports: ["5003:5000"]
    volumes: ["./BE/zone_service/osrm_data/osrm-no-recommend:/data:ro"]
  
  osrm-base:
    ports: ["5004:5000"]
    volumes: ["./BE/zone_service/osrm_data/osrm-base:/data:ro"]
```

### 4. Environment Variables

```bash
# BE/zone_service/env.local
OSRM_PRIORITY_FIRST_URL=http://localhost:5000
OSRM_SPEED_LEANING_URL=http://localhost:5001
OSRM_BALANCED_URL=http://localhost:5002
OSRM_NO_RECOMMEND_URL=http://localhost:5003
OSRM_BASE_URL=http://localhost:5004
```

### 5. Frontend Integration

#### Routing Store (`useRouting.ts`)
```typescript
const routingMode = ref<'priority_first' | 'speed_leaning' | 'balanced' | 'no_recommend' | 'base'>('balanced')

const response = await calculateDemoRoute({
  startPoint: startPoint.value!,
  priorityGroups: nonEmptyGroups,
  steps: true,
  annotations: true,
  mode: routingMode.value,
})
```

#### UI Selector (`DemoRoutingView.vue`)
- Radio group with 5 mode options
- Each mode has icon and description
- Real-time mode switching without page reload

## Generation Workflow

### Step 1: Generate OSRM Data
```bash
cd BE/zone_service
npm run generate-osrm
```

This will:
1. Fetch real-time traffic data from TomTom
2. Load `road_overrides` from database
3. For each instance:
   - Export road network to OSM XML with mode-specific `knp:*` tags
   - Generate Lua profile with mode parameter
   - Run OSRM processing (extract → partition → customize)
4. Output 5 complete OSRM datasets

### Step 2: Start OSRM Containers
```bash
# Start all 5 instances
docker-compose up osrm-priority-first osrm-speed-leaning osrm-balanced osrm-no-recommend osrm-base -d

# Or start all services
docker-compose up -d
```

### Step 3: Verify Instances
```bash
# Check instance 1 (priority-first)
curl http://localhost:5000/route/v1/motorbike/106.660172,10.762622;106.680172,10.782622

# Check instance 2 (speed-leaning)
curl http://localhost:5001/route/v1/motorbike/106.660172,10.762622;106.680172,10.782622

# ... repeat for ports 5002, 5003, 5004
```

## KNP Override Tags

### Tags Applied to OSM XML

| Tag | Type | Description | Applied In Mode |
|-----|------|-------------|-----------------|
| `knp:mode` | string | Routing mode identifier | All |
| `knp:block_level` | enum | Block severity (soft, min, hard) | All except base |
| `knp:delta` | float | Weight adjustment | All except base, no_recommend |
| `knp:point_score` | float | Road quality score (0-1) | All except base, no_recommend |
| `knp:recommend_enabled` | boolean | Enable/disable recommendations | All except base |
| `knp:soft_penalty` | float | Soft block penalty multiplier | All except base |
| `knp:min_penalty` | float | Min block penalty multiplier | All except base |

### Lua Profile Processing

```lua
-- Read override tags
local knp_block_level = way:get_value_by_key("knp:block_level")
local knp_delta = tonumber(way:get_value_by_key("knp:delta")) or 0
local knp_point_score = tonumber(way:get_value_by_key("knp:point_score"))
local knp_recommend_enabled = way:get_value_by_key("knp:recommend_enabled")

-- Hard block check
if knp_block_level == "hard" then
  return  -- Road is inaccessible
end

-- Weight calculation
local adjusted_weight = custom_weight * shipper_penalty

-- Apply point score
if knp_point_score and knp_recommend_enabled ~= "0" then
  local point_factor = 2.0 - knp_point_score
  adjusted_weight = adjusted_weight * point_factor
end

-- Apply delta
adjusted_weight = adjusted_weight + knp_delta

-- Apply block penalties
if knp_block_level == "soft" then
  adjusted_weight = adjusted_weight * knp_soft_penalty
elseif knp_block_level == "min" then
  adjusted_weight = adjusted_weight * knp_min_penalty
end
```

## Testing Comparison

You can test all 5 modes on the same route to compare:

```bash
# Same coordinates, different modes
START="106.660172,10.762622"
END="106.680172,10.782622"

curl "http://localhost:5000/route/v1/motorbike/${START};${END}" > priority-first.json
curl "http://localhost:5001/route/v1/motorbike/${START};${END}" > speed-leaning.json
curl "http://localhost:5002/route/v1/motorbike/${START};${END}" > balanced.json
curl "http://localhost:5003/route/v1/motorbike/${START};${END}" > no-recommend.json
curl "http://localhost:5004/route/v1/motorbike/${START};${END}" > base.json

# Compare distances
jq -r '.routes[0].distance' priority-first.json
jq -r '.routes[0].distance' speed-leaning.json
jq -r '.routes[0].distance' balanced.json
jq -r '.routes[0].distance' no-recommend.json
jq -r '.routes[0].distance' base.json
```

## Performance Characteristics

| Mode | Query Speed | Route Quality | Priority Adherence |
|------|-------------|---------------|-------------------|
| priority-first | Medium | High | ★★★★★ |
| speed-leaning | Fast | Medium | ★★☆☆☆ |
| balanced | Medium | High | ★★★★☆ |
| no-recommend | Fast | Medium | ★★★☆☆ |
| base | Fast | Low-Medium | N/A |

## Maintenance

### Rebuild Single Instance
```bash
# If you need to rebuild just one instance:
cd BE/zone_service
# Edit generate-osrm-data.ts to only include desired instance
npm run generate-osrm

# Restart container
docker-compose restart osrm-balanced
```

### Hot-Swap with osrm-datastore
For fast updates without full rebuild (future MLD enhancement):
```bash
# Generate dynamic_updates.csv
# Run osrm-customize --segment-speed-file
# Use osrm-datastore for hot-swap
```

## Troubleshooting

### Instance Won't Start
- Check OSRM data exists: `ls BE/zone_service/osrm_data/osrm-*/network.osrm*`
- Check Docker logs: `docker logs dss-osrm-balanced`
- Verify port not in use: `netstat -an | grep 5002`

### Route Quality Issues
- Check override data: `SELECT * FROM road_overrides LIMIT 10;`
- Verify mode tags in XML: `grep "knp:mode" osrm_data/osrm-balanced/network.osm.xml | head`
- Compare with base mode to isolate override issues

### Performance Problems
- Check OSRM container resources: `docker stats`
- Monitor query latency: `curl -w "@curl-format.txt" ...`
- Consider increasing `--max-table-size` in docker-compose.yml

## Future Enhancements

1. **Dynamic Lambda Adjustment**: Allow λ to be tuned per request
2. **MLD Fast Updates**: Implement `osrm-customize` + `osrm-datastore` pipeline
3. **A/B Testing**: Track which mode performs best for different scenarios
4. **Machine Learning**: Use historical data to auto-select best mode
5. **Hybrid Routing**: Combine multiple modes in a single route

## References

- [OSRM Operations Guide](./OSRM_OPERATIONS_GUIDE.md)
- [Overlay XML Schema](./OVERLAY_XML_SCHEMA.md)
- [Implementation Summary](../IMPLEMENTATION_SUMMARY.md)
