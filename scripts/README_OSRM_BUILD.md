# OSRM Build Instructions

## ⚠️ Important: Manual Build Only

OSRM data builds are **NOT automatic**. You must manually trigger builds through the **osrm-management-system** service.

## How to Build OSRM Data

### Option 1: Via API (Recommended)

Use the osrm-management-system API to build OSRM data:

```bash
# Extract OSM data first (if needed)
curl -X POST http://localhost:21520/api/v1/extract/complete

# Generate OSRM V2 models from database
curl -X POST http://localhost:21520/api/v1/generate/osrm-v2
```

### Option 2: Via ManagementSystem UI

1. Navigate to System Management page
2. Click "Generate OSRM Data (All Models)"
3. Monitor build status in the UI

### Option 3: Check Build Status

```bash
# Get build status for all models
curl http://localhost:21520/api/v1/builds/status

# Get build status for specific model
curl http://localhost:21520/api/v1/builds/status/osrm-full

# Get build history
curl http://localhost:21520/api/v1/builds/history
```

## Build Process

1. **Extract OSM Data** (if needed):
   - Extracts routing graph and addresses from Vietnam PBF file
   - Uses polygon file to clip to specific area
   - Output: `raw_data/extracted/thuduc_complete.osm.pbf`

2. **Generate OSRM Models**:
   - Reads road network data from database
   - Generates 4 OSRM models:
     - `osrm-full`: Rating + Blocking
     - `osrm-rating-only`: Rating only
     - `osrm-blocking-only`: Blocking only
     - `osrm-base`: Base model (no modifiers)
   - Builds are tracked in `osrm_builds` database table

3. **Deploy** (after build completes):
   - Rebuild containers to use new data:
   ```bash
   docker-compose -p dss -f docker-compose.yml restart osrm-v2-full
   docker-compose -p dss -f docker-compose.yml restart osrm-v2-rating-only
   docker-compose -p dss -f docker-compose.yml restart osrm-v2-blocking-only
   docker-compose -p dss -f docker-compose.yml restart osrm-v2-base
   ```

## Build Status

Builds are tracked in the database (`osrm_builds` table) with statuses:
- `PENDING`: Build queued
- `BUILDING`: Currently building
- `READY`: Build completed, ready to deploy
- `DEPLOYED`: Build deployed to containers
- `FAILED`: Build failed
- `DEPRECATED`: Build superseded by newer build

## Notes

- Builds run **sequentially** (one at a time per model)
- Build status is available via API and ManagementSystem UI
- Container management (start/stop/restart) is handled by osrm-management-system
- No automatic builds on container startup
