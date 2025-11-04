# OSRM Motorbike Implementation with Dynamic Routing

## Overview

This implementation provides a complete motorbike-first OSRM routing system with 5 key components:

1. **OSRM Graph (motorbike)** - Core routing engine with override support
2. **Sequencer Priority-first** - Always visits high-priority waypoints
3. **Sequencer Speed-leaning** - Skips low-priority waypoints when detour is large
4. **Road Recommendation Toggle** - Per-road enable/disable of scoring
5. **Blocking Policy Model** - Customizable penalties (soft/min/hard)

## Quick Start

### 1. Database Setup

Run migrations to add spatial indexes and override tables:

```bash
cd BE/zone_service
npx prisma migrate deploy
```

### 2. Generate OSRM Data

```bash
npm run generate:osrm
```

This will:
- Export road network from database to OSM XML
- Merge road overrides into XML tags (`knp:*`)
- Generate custom motorbike Lua profile
- Process OSRM data (extract → partition → customize)
- Create routing files in `osrm_data/osrm-instance-2/`

### 3. Start OSRM Server

```bash
docker-compose up osrm-instance-2
```

OSRM will be available at `http://localhost:5001`

## Architecture

### Database Schema

```
road_segments
├── segment_id (PK)
├── osm_way_id (INDEX)
├── geometry_linestring (SPATIAL INDEX, SRID 4326)
├── base_weight, delta_weight
└── [existing fields...]

road_overrides
├── override_id (PK)
├── segment_id / osm_way_id
├── block_level (none/soft/min/hard)
├── delta (additive weight adjustment)
├── point_score (0-1, road quality)
├── recommend_enabled (boolean)
├── soft_penalty_factor, min_penalty_factor
└── updated_by, updated_at

poi_priorities
├── priority_id (PK)
├── poi_id (UNIQUE)
├── priority (1-5, 1=highest)
├── latitude, longitude
└── time_windows (JSON)
```

### Services

- **`segment-lookup-service.ts`** - Fast segment lookup (by ID, OSM way, or coordinates)
- **`override-management-service.ts`** - CRUD for overrides and POIs, trigger rebuild
- **`sequencing-service.ts`** - Waypoint sequencing with 2 modes

## Usage Examples

### Find and Block a Road

```typescript
import { segmentLookupService, overrideManagementService } from './services';

// Find segments by clicking on map
const segments = await segmentLookupService.snapPoint({
  latitude: 10.8231,
  longitude: 106.6297,
  threshold_meters: 25,
});

// Block the nearest segment
await overrideManagementService.upsertRoadOverride({
  segment_id: segments[0].segment_id,
  block_level: 'hard',
  updated_by: 'admin',
});

// Rebuild OSRM to apply
await overrideManagementService.triggerRebuild();
```

### Improve Road Based on Feedback

```typescript
// Good shipper feedback = improve road
await overrideManagementService.upsertRoadOverride({
  segment_id: 'abc-123',
  point_score: 0.9,      // 0.9 = excellent road
  delta: -0.2,           // Reduce weight by 0.2
  recommend_enabled: true,
  updated_by: 'feedback_system',
});
```

### Sequence Waypoints (Priority-First)

```typescript
import { sequencingService } from './services';

const result = await sequencingService.sequence({
  start: { latitude: 10.8231, longitude: 106.6297 },
  waypoints: [
    { id: 'hospital', latitude: 10.8235, longitude: 106.6300, priority: 1 },
    { id: 'shop_a', latitude: 10.8240, longitude: 106.6305, priority: 3 },
    { id: 'shop_b', latitude: 10.8245, longitude: 106.6308, priority: 5 },
  ],
  mode: 'priority_first',
  lambda: 1.0,
});

console.log('Visit order:', result.ordered_waypoints.map(w => w.id));
// ['hospital', 'shop_a', 'shop_b']
```

### Sequence Waypoints (Speed-Leaning)

```typescript
const result = await sequencingService.sequence({
  start: { latitude: 10.8231, longitude: 106.6297 },
  waypoints: [...],
  mode: 'speed_leaning',
  lambda: 0.2,  // Low λ = aggressive skipping
});

console.log('Visited:', result.ordered_waypoints.map(w => w.id));
console.log('Skipped:', result.skipped_waypoints.map(w => w.id));
// Visited: ['hospital', 'shop_a']
// Skipped: ['shop_b']  // Too far for low priority
```

## Key Concepts

### Block Levels

- **none**: No blocking (default)
- **soft**: 2x weight penalty (discourage but allow)
- **min**: 5x weight penalty (avoid unless necessary)
- **hard**: Completely inaccessible

### Point Score (0-1)

Road quality score that affects weight:
- `1.0`: Excellent road → 50% weight reduction
- `0.5`: Neutral → no change
- `0.0`: Poor road → 100% weight increase

Formula: `adjusted_weight = base_weight * (2.0 - point_score)`

### Delta

Direct additive adjustment to weight:
- Positive delta: increase weight (discourage)
- Negative delta: decrease weight (encourage)

### Recommend Enabled

Boolean flag to enable/disable `point_score` and `delta`:
- `true`: Apply scoring
- `false`: Ignore scoring, use base values only

### Sequencing Modes

**Priority-first** (λ = 1.0):
- Always visits high-priority waypoints
- Accepts larger detours
- Use for medical deliveries, critical stops

**Speed-leaning** (λ = 0.2):
- Prefers fast routes
- Skips low-priority waypoints if detour is large
- Use for time-sensitive deliveries with optional stops

Formula: `score = priority_benefit - λ * detour_cost`

## MySQL Spatial Queries

### Point Snapping (25m threshold)

```typescript
const segments = await segmentLookupService.snapPoint({
  latitude: 10.8231,
  longitude: 106.6297,
  threshold_meters: 25,
  max_results: 5,
});
```

### Polyline Intersection (20m buffer)

```typescript
const segments = await segmentLookupService.snapPolyline({
  coordinates: [
    [106.6297, 10.8231],
    [106.6300, 10.8235],
  ],
  buffer_meters: 20,
  max_results: 50,
});
```

### Bounding Box Query

```typescript
const segments = await segmentLookupService.getSegmentsInBBox(
  10.82,   // minLat
  106.62,  // minLon
  10.83,   // maxLat
  106.63,  // maxLon
  500      // limit
);
```

## Rebuild Process

When overrides are updated, OSRM must be rebuilt to apply changes:

```typescript
await overrideManagementService.triggerRebuild();
```

**Steps:**
1. Export database to OSM XML
2. Merge overrides into XML tags (`knp:*`)
3. Generate motorbike Lua profile
4. Run OSRM extract → partition → customize
5. Restart OSRM server (or hot-swap if MLD)

**Duration:** 5-15 minutes depending on data size

**Note:** The current implementation uses full rebuild. For faster updates, see optional MLD fast path in docs.

## File Structure

```
BE/zone_service/
├── prisma/
│   ├── models/
│   │   ├── road_segments.prisma      # Extended with spatial geometry
│   │   ├── road_overrides.prisma     # Override management
│   │   └── poi_priorities.prisma     # POI priority management
│   └── migrations/
│       └── 20250103_add_spatial_and_overrides/
│           └── migration.sql         # Spatial + overrides schema
├── services/
│   ├── segment-lookup-service.ts     # Fast segment lookup
│   ├── override-management-service.ts # CRUD for overrides/POIs
│   └── sequencing-service.ts         # Waypoint sequencing (2 modes)
├── processors/
│   └── generate-osrm-data.ts         # Updated for motorbike-only + overrides
├── docs/
│   ├── OSRM_OPERATIONS_GUIDE.md     # Complete operations guide
│   └── OVERLAY_XML_SCHEMA.md        # XML schema documentation
└── osrm_data/
    └── osrm-instance-2/              # Motorbike OSRM data
        ├── network.osm.xml           # Generated OSM XML with knp:* tags
        ├── custom_motorbike.lua      # Profile with override logic
        └── network.osrm*             # OSRM routing files
```

## Performance

- **Spatial lookups**: ~5-20ms with SPATIAL INDEX
- **OSRM /table query**: ~50-200ms for 10 waypoints
- **OSRM /route query**: ~30-100ms per route
- **Sequencing (greedy)**: ~100-500ms for 20 waypoints
- **OSRM rebuild**: 5-15 minutes (city-scale data)

## Troubleshooting

### Spatial Queries Slow

```sql
-- Verify SPATIAL INDEX exists
SHOW INDEX FROM road_segments WHERE Key_name = 'idx_geometry_spatial';

-- Rebuild if needed
ALTER TABLE road_segments DROP INDEX idx_geometry_spatial;
ALTER TABLE road_segments ADD SPATIAL INDEX idx_geometry_spatial(geometry_linestring);
```

### Overrides Not Applied

1. Check override exists: `SELECT * FROM road_overrides WHERE segment_id = 'xxx';`
2. Verify `recommend_enabled = 1`
3. Rebuild OSRM
4. Check Lua profile during extract

### OSRM Rebuild Fails

1. Check Docker: `docker ps | grep osrm`
2. Check disk space: `df -h`
3. Review logs: `docker logs osrm-instance-2`
4. Verify XML exists: `ls -lh osrm_data/osrm-instance-2/network.osrm.xml`

## Documentation

- **[OSRM Operations Guide](docs/OSRM_OPERATIONS_GUIDE.md)** - Complete operational guide
- **[Overlay XML Schema](docs/OVERLAY_XML_SCHEMA.md)** - XML schema and examples
- **[car.lua Analysis Report](raw_data/osrm-logic/car_profile_traversal_report.md)** - Profile analysis

## Optional: MLD Fast Path

For faster updates without full rebuild, implement MLD customize path:

1. Generate `dynamic_updates.csv` from overrides
2. Run `osrm-customize --segment-speed-file dynamic_updates.csv`
3. Run `osrm-datastore` for hot-swap

See `docs/OSRM_OPERATIONS_GUIDE.md` for details.

## Future Enhancements

- **ILP-based sequencing** for optimal multi-point routes
- **MLD fast path** for sub-minute override updates
- **Time-dependent routing** based on POI time windows
- **Multi-vehicle optimization** for fleet routing
- **Real-time traffic integration** with TomTom API
- **H3/S2 spatial indexing** for very large datasets

## License

Internal project for graduation thesis.

## Contact

For questions or issues, contact the development team.
