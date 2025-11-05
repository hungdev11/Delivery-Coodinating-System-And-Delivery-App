# OSRM Operations Guide

## Overview

This guide covers operations for the motorbike-only OSRM instance with dynamic routing adjustments through road overrides and POI priorities.

## Architecture

### 5 Components

1. **OSRM Graph (motorbike)**: Core routing engine that respects overrides
2. **Sequencer Priority-first**: Always visits high-priority waypoints
3. **Sequencer Speed-leaning**: Skips low-priority waypoints if detour is large
4. **Road Recommendation Toggle**: Per-road enable/disable of point_score/delta
5. **Blocking Policy Model**: Customizable penalties for soft/min/hard blocks

### MySQL Spatial Lookup

- Fast segment lookup by `segment_id`, `osm_way_id`, or geographic coordinates
- Uses SPATIAL INDEX on `geometry_linestring` column
- Supports point snapping (20-30m threshold) and polyline buffering (15-25m)

---

## Database Schema

### Tables

#### `road_segments` (extended)
- `osm_way_id` BIGINT: OSM way ID for mapping
- `geometry_linestring` GEOMETRY(LineString, 4326): Spatial geometry with SPATIAL INDEX

#### `road_overrides`
```sql
CREATE TABLE road_overrides (
  override_id VARCHAR(36) PRIMARY KEY,
  segment_id VARCHAR(36),
  osm_way_id BIGINT,
  block_level ENUM('none','soft','min','hard') DEFAULT 'none',
  delta FLOAT,                    -- Additive weight adjustment
  point_score FLOAT,              -- 0-1 (higher = better road)
  recommend_enabled TINYINT(1) DEFAULT 1,
  soft_penalty_factor FLOAT,     -- Custom soft block penalty
  min_penalty_factor FLOAT,      -- Custom min block penalty
  updated_by VARCHAR(255),
  updated_at DATETIME,
  created_at DATETIME
);
```

#### `poi_priorities`
```sql
CREATE TABLE poi_priorities (
  priority_id VARCHAR(36) PRIMARY KEY,
  poi_id VARCHAR(255) UNIQUE,
  poi_name VARCHAR(255),
  poi_type VARCHAR(100),
  priority INT DEFAULT 3,        -- 1=highest, 5=lowest
  time_windows JSON,             -- Optional time windows
  latitude DECIMAL(10,7),
  longitude DECIMAL(10,7),
  updated_by VARCHAR(255),
  updated_at DATETIME,
  created_at DATETIME
);
```

---

## Road Override System

### Block Levels

- **none**: No blocking (default)
- **soft**: Slight penalty (default 2x weight), discourage but allow
- **min**: Moderate penalty (default 5x weight), avoid unless necessary
- **hard**: Complete block, road is inaccessible

### Point Score (0-1)

Higher score = better road quality (reduces weight)
- `1.0`: Excellent road (50% weight reduction)
- `0.5`: Neutral (no change)
- `0.0`: Poor road (100% weight increase)

Formula in Lua profile:
```lua
point_factor = 2.0 - point_score
adjusted_weight = base_weight * point_factor
```

### Delta

Direct additive adjustment to road weight (in abstract units).
- Positive delta: increase weight (discourage)
- Negative delta: decrease weight (encourage)

### Recommend Enabled

Boolean flag to enable/disable `point_score` and `delta` for a road.
- `true` (1): Apply point_score and delta
- `false` (0): Ignore point_score and delta, use base values only

---

## Operations

### 1. Create/Update Road Override

```typescript
import { overrideManagementService } from './services/override-management-service';

// Block a road (hard block)
await overrideManagementService.upsertRoadOverride({
  segment_id: 'abc-123',
  block_level: 'hard',
  updated_by: 'admin',
});

// Improve a road with good feedback
await overrideManagementService.upsertRoadOverride({
  segment_id: 'def-456',
  point_score: 0.9,      // Good road
  delta: -0.2,           // Slight weight reduction
  recommend_enabled: true,
  updated_by: 'system',
});

// Soft block with custom penalty
await overrideManagementService.upsertRoadOverride({
  osm_way_id: 123456789,
  block_level: 'soft',
  soft_penalty_factor: 3.0,  // Custom 3x penalty
  updated_by: 'admin',
});
```

### 2. Find Segments to Update

```typescript
import { segmentLookupService } from './services/segment-lookup-service';

// By segment ID
const segment = await segmentLookupService.getBySegmentId('abc-123');

// By OSM way ID
const segments = await segmentLookupService.getByOsmWayId(123456789);

// By clicking on map (point snapping)
const nearbySegments = await segmentLookupService.snapPoint({
  latitude: 10.8231,
  longitude: 106.6297,
  threshold_meters: 25,
  max_results: 5,
});

// By drawing a polyline (buffer + intersection)
const intersectingSegments = await segmentLookupService.snapPolyline({
  coordinates: [
    [106.6297, 10.8231],
    [106.6300, 10.8235],
    [106.6305, 10.8240],
  ],
  buffer_meters: 20,
  max_results: 50,
});
```

### 3. Batch Update Multiple Roads

```typescript
// Block all segments along a road
const segments = await segmentLookupService.getByOsmWayId(123456789);
const segmentIds = segments.map(s => s.segment_id);

await overrideManagementService.batchUpdateRoadOverrides(segmentIds, {
  block_level: 'soft',
  updated_by: 'admin',
});
```

### 4. Manage POI Priorities

```typescript
// Create POI with high priority
await overrideManagementService.createPOIPriority({
  poi_id: 'hospital-001',
  poi_name: 'Central Hospital',
  poi_type: 'hospital',
  priority: 1,  // Highest
  latitude: 10.8231,
  longitude: 106.6297,
  updated_by: 'admin',
});

// Update POI priority with time windows
await overrideManagementService.updatePOIPriority({
  priority_id: 'xxx-yyy',
  priority: 2,
  time_windows: [
    { start: '08:00', end: '18:00', days: [1, 2, 3, 4, 5] }, // Weekdays
  ],
});
```

### 5. Trigger OSRM Rebuild

After updating overrides, rebuild OSRM to apply changes:

```typescript
const result = await overrideManagementService.triggerRebuild();
console.log(result.message);
```

**Command line alternative:**
```bash
cd BE/zone_service
npm run generate:osrm
```

**Expected duration:** 5-15 minutes depending on data size.

---

## Sequencing Waypoints

### Priority-First Mode

Always visits high-priority waypoints, accepts larger detours.

```typescript
import { sequencingService } from './services/sequencing-service';

const result = await sequencingService.sequence({
  start: { latitude: 10.8231, longitude: 106.6297 },
  end: { latitude: 10.8250, longitude: 106.6310 },
  waypoints: [
    { id: 'wp1', latitude: 10.8235, longitude: 106.6300, priority: 1, name: 'Hospital' },
    { id: 'wp2', latitude: 10.8240, longitude: 106.6305, priority: 3, name: 'Shop A' },
    { id: 'wp3', latitude: 10.8245, longitude: 106.6308, priority: 5, name: 'Shop B' },
  ],
  mode: 'priority_first',
  lambda: 1.0,  // High λ: prioritize waypoints over speed
});

console.log('Ordered waypoints:', result.ordered_waypoints);
console.log('Total duration:', result.total_duration, 'seconds');
```

### Speed-Leaning Mode

Prefers fast routes, skips low-priority waypoints if detour is large.

```typescript
const result = await sequencingService.sequence({
  start: { latitude: 10.8231, longitude: 106.6297 },
  waypoints: [...],
  mode: 'speed_leaning',
  lambda: 0.2,  // Low λ: prefer speed, skip low-priority if detour is large
});

console.log('Visited:', result.ordered_waypoints);
console.log('Skipped:', result.skipped_waypoints);
```

### Lambda Parameter

- **Priority-first**: λ = 0.8-1.5 (higher = stricter priority enforcement)
- **Speed-leaning**: λ = 0.1-0.3 (lower = more aggressive skipping)

Formula: `score = priority_benefit - λ * detour_cost`

---

## MySQL Spatial Queries

### Point Snapping (20-30m threshold)

```sql
SELECT 
  segment_id,
  name,
  ST_Distance_Sphere(
    geometry_linestring,
    ST_GeomFromText('POINT(106.6297 10.8231)', 4326)
  ) as distance_meters
FROM road_segments
WHERE geometry_linestring IS NOT NULL
  AND ST_Distance_Sphere(
    geometry_linestring,
    ST_GeomFromText('POINT(106.6297 10.8231)', 4326)
  ) <= 25
ORDER BY distance_meters ASC
LIMIT 5;
```

### Polyline Intersection (buffer 15-25m)

```sql
SELECT segment_id, name
FROM road_segments
WHERE geometry_linestring IS NOT NULL
  AND ST_Intersects(
    ST_Buffer(geometry_linestring, 20 / 111320),
    ST_Buffer(
      ST_GeomFromText('LINESTRING(106.6297 10.8231, 106.6300 10.8235)', 4326),
      20 / 111320
    )
  );
```

**Note:** Divide buffer by 111,320 to convert meters to degrees at equator.

---

## Monitoring & Stats

### Get Override Statistics

```typescript
const stats = await overrideManagementService.getRebuildStats();
console.log(stats);
// {
//   total_overrides: 150,
//   total_poi_priorities: 25,
//   blocked_roads: 12,
//   recommendation_disabled: 8
// }
```

### List Active Overrides

```typescript
const overrides = await overrideManagementService.listRoadOverrides({
  block_level: 'hard',
  limit: 50,
});
```

---

## Troubleshooting

### OSRM Rebuild Fails

1. Check Docker is running: `docker ps | grep osrm`
2. Check disk space: `df -h`
3. Review logs: `docker logs osrm-instance-2`
4. Verify XML file exists: `ls -lh BE/zone_service/osrm_data/osrm-instance-2/network.osm.xml`

### Spatial Queries Slow

1. Verify SPATIAL INDEX exists:
```sql
SHOW INDEX FROM road_segments WHERE Key_name = 'idx_geometry_spatial';
```

2. Rebuild index if needed:
```sql
ALTER TABLE road_segments DROP INDEX idx_geometry_spatial;
ALTER TABLE road_segments ADD SPATIAL INDEX idx_geometry_spatial(geometry_linestring);
```

3. Ensure `geometry_linestring` is populated:
```sql
SELECT COUNT(*) FROM road_segments WHERE geometry_linestring IS NOT NULL;
```

### Overrides Not Applied

1. Check override exists: `SELECT * FROM road_overrides WHERE segment_id = 'xxx';`
2. Verify `recommend_enabled = 1` if using point_score/delta
3. Rebuild OSRM to apply changes
4. Check Lua profile logs during extract

---

## Rollback Procedures

### Revert Single Override

```typescript
await overrideManagementService.deleteRoadOverride('override-id');
await overrideManagementService.triggerRebuild();
```

### Bulk Disable Recommendations

```typescript
const segmentIds = [...]; // List of segments
await overrideManagementService.batchUpdateRoadOverrides(segmentIds, {
  recommend_enabled: false,
  updated_by: 'admin',
});
```

### Clear All Blocks

```sql
UPDATE road_overrides SET block_level = 'none' WHERE block_level IN ('soft', 'min', 'hard');
```

Then rebuild OSRM.

---

## Best Practices

1. **Always test overrides on staging first**
2. **Use soft blocks before hard blocks** to see impact
3. **Document reason for each override** in `updated_by` field
4. **Monitor rebuild duration** and schedule during low-traffic hours
5. **Keep POI priorities up to date** (review quarterly)
6. **Use batch operations** for multiple segment updates
7. **Snapshot database** before large override changes

---

## Performance Tips

- **Spatial queries**: Keep threshold ≤ 50m for fast results
- **Batch overrides**: Group updates and rebuild once
- **Lambda tuning**: Test different values with real data
- **OSRM memory**: Ensure sufficient RAM for .osrm files (2-4GB for city-scale)
- **Index maintenance**: Rebuild spatial indexes monthly if heavy updates

---

## Contact & Support

For issues or questions, contact the zone-service team or file an issue in the project repository.
