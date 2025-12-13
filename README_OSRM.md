# OSRM Build and Management Guide

This guide explains how to build and manage OSRM routing data for the production environment.

## Overview

The OSRM system uses 4 different models:
- **osrm-full**: Rating affects weight, Blocking affects speed
- **osrm-rating-only**: User feedback affects weight only
- **osrm-blocking-only**: Traffic affects speed only
- **osrm-base**: VN motorbike optimized, no modifiers

## Prerequisites

1. **osmium-tool**: For extracting OSM data from PBF files
   - Install: `sudo apt-get install osmium-tool` (Ubuntu/Debian)
   - Or use Docker fallback (enabled by default)

2. **Docker**: Required for OSRM build tools
   - OSRM tools run in Docker containers

3. **Zone Service**: Must have database with road network data
   - The generator reads from database to build OSRM data

## First-Time Setup

### Option 1: Build on Production Server (Recommended for Small Data)

If you have the zone-service container running and database accessible:

```bash
cd prod-pack
./scripts/build-osrm.sh
```

This will:
1. Extract OSM data using Docker (avoids memory issues)
2. Use zone-service container to generate OSRM data from database
3. Build all 4 OSRM models
4. Copy generated data to `prod-pack/osrm_data/`
5. Copy lib folder to each instance
6. Start all OSRM containers

**Note**: The script always uses Docker for osmium to prevent out-of-memory errors. It will automatically try multiple Docker images if one fails.

### Option 2: Build on Separate Machine (Recommended for Large Data)

For production servers with limited resources, build on a separate machine:

**On Build Machine (with zone_service source code):**
```bash
cd BE/zone_service
npm install
npm run osrm:generate
# This creates osrm_data/ with all models
```

**Transfer to Production:**
```bash
# On build machine
tar -czf osrm_data.tar.gz osrm_data/

# On production server
cd prod-pack
./scripts/build-osrm.sh --use-prebuilt /path/to/osrm_data
```

### Option 3: Use Pre-extracted OSM File

If you already have extracted OSM data (from another machine or previous run):

```bash
cd prod-pack
./scripts/build-osrm.sh --extracted-file /path/to/thuduc_complete.osm.pbf
```

This will:
- Copy your extracted file to the correct location
- Verify the file is valid
- Skip extraction (no memory issues!)
- Continue with OSRM data generation

**Benefits:**
- No memory issues (extraction already done)
- Fast (just copy and verify)
- Can extract on powerful machine, use on low-memory server

### Option 4: Use Pre-built OSRM Data

If you already have complete OSRM data built:

```bash
cd prod-pack
./scripts/build-osrm.sh --use-prebuilt /path/to/osrm_data
```

## Rebuilding with New Data

When you update the database (new roads, ratings, traffic data), you can regenerate OSRM data:

**Option 1: Via API (Recommended)**
```bash
curl -X POST http://localhost:8080/api/v1/osrm/generate-v2
```

**Option 2: Via Script**
```bash
cd prod-pack
./scripts/build-osrm.sh --skip-extract
```

This will:
1. Use zone-service container to generate OSRM data from database
2. Build all 4 OSRM models
3. Copy generated data to `prod-pack/osrm_data/`
4. Start all OSRM containers

**Note:** The old workflow (build individual instances, start/stop, rolling restart) has been removed. Now all 4 models are built together from the database via API endpoint.

## Manual Steps

### Extract OSM Data Only

```bash
cd prod-pack
./scripts/build-osrm.sh --skip-start
```

### Build OSRM Without Starting Containers

```bash
cd prod-pack
./scripts/build-osrm.sh --skip-extract --skip-start
```

### Start OSRM Containers Only

```bash
cd prod-pack
docker-compose up -d osrm-v2-full osrm-v2-rating-only osrm-v2-blocking-only osrm-v2-base
```

### Check OSRM Container Status

```bash
docker-compose ps osrm-v2-full osrm-v2-rating-only osrm-v2-blocking-only osrm-v2-base
```

### View OSRM Logs

```bash
docker-compose logs -f osrm-v2-full
```

## Directory Structure

```
prod-pack/
├── osrm_data/
│   ├── lib/                    # OSRM Lua library files
│   ├── osrm-full/              # Full model data
│   ├── osrm-rating-only/       # Rating-only model data
│   ├── osrm-blocking-only/     # Blocking-only model data
│   └── osrm-base/              # Base model data
├── raw_data/
│   ├── vietnam/                # Source Vietnam PBF
│   ├── poly/                   # Polygon boundary files
│   └── extracted/              # Extracted OSM data
└── scripts/
    └── build-osrm.sh           # Main build script
```

## Environment Variables

You can customize paths and behavior using environment variables:

```bash
export OSRM_DATA_DIR=/path/to/osrm_data
export RAW_DATA_DIR=/path/to/raw_data
export OSMIUM_MEMORY_LIMIT=4g  # Memory limit for osmium Docker container (default: 4g)
```

## Troubleshooting

### osmium-tool killed (Out of Memory)

The script always uses Docker for osmium with memory limits. If you still get OOM errors:

```bash
# Increase memory limit (default: 4g)
export OSMIUM_MEMORY_LIMIT=8g
./scripts/build-osrm.sh
```

Or build on a machine with more RAM and transfer the data.

### Docker image pull failed

If you get "denied" or "not found" errors when pulling the osmium Docker image:

The script automatically tries multiple images:
1. `danieljh/osmium:latest` (public, recommended)
2. `ghcr.io/osmcode/osmium-tool:latest` (may require auth)

If both fail, you can:
- Check Docker connectivity: `docker pull danieljh/osmium:latest`
- Use pre-extracted OSM data (skip extraction step)
- Build on a different machine with better Docker access

### zone-service container not found

If you're on production and don't have source code:

1. **Option 1**: Build on separate machine and transfer
   ```bash
   # On build machine with source code
   cd BE/zone_service && npm run osrm:generate
   # Transfer osrm_data/ to production
   ```

2. **Option 2**: Use pre-built data
   ```bash
   ./scripts/build-osrm.sh --use-prebuilt /path/to/osrm_data
   ```

3. **Option 3**: Start zone-service container first
   ```bash
   docker-compose up -d zone-service
   # Wait for it to be healthy
   ./scripts/build-osrm.sh --skip-extract
   ```

### OSRM build fails

1. Check that database has road network data:
   ```bash
   cd BE/zone_service
   npm run prisma:studio
   ```

2. Verify extracted OSM file exists:
   ```bash
   ls -lh prod-pack/raw_data/extracted/thuduc_complete.osm.pbf
   ```

3. Check OSRM container logs:
   ```bash
   docker-compose logs osrm-v2-full
   ```

### Lib folder missing

The lib folder should be automatically copied. If missing:

```bash
# Copy from zone_service
cp -r BE/zone_service/osrm_data/lib prod-pack/osrm_data/

# Or copy to each instance
for model in osrm-full osrm-rating-only osrm-blocking-only osrm-base; do
  cp -r prod-pack/osrm_data/lib prod-pack/osrm_data/$model/
done
```

## Using Your Own VPS as Builder

To build OSRM data on a separate VPS (recommended for production):

1. **On VPS**: Install dependencies
   ```bash
   # Install Docker (osmium will use Docker)
   curl -fsSL https://get.docker.com | sh
   
   # Clone repository
   git clone <your-repo>
   ```

2. **On VPS**: Build OSRM data
   ```bash
   cd DS/BE/zone_service
   npm install
   
   # Ensure database connection is configured
   # Then generate OSRM data
   npm run osrm:generate
   ```

3. **Transfer data**: Copy `osrm_data/` to production server
   ```bash
   # On VPS
   cd DS/BE/zone_service
   tar -czf osrm_data.tar.gz osrm_data/
   
   # Transfer to production
   scp osrm_data.tar.gz user@production:/tmp/
   
   # On production server
   cd /www/server/panel/data/compose/dss
   mkdir -p osrm_data
   tar -xzf /tmp/osrm_data.tar.gz -C osrm_data --strip-components=1
   
   # Use pre-built data
   ./scripts/build-osrm.sh --use-prebuilt osrm_data --skip-extract
   ```

## API Usage

Once OSRM containers are running, they're accessible via nginx:

- Full model: `http://localhost:8080/osrm/full/route/v1/driving/...`
- Rating only: `http://localhost:8080/osrm/rating-only/route/v1/driving/...`
- Blocking only: `http://localhost:8080/osrm/blocking-only/route/v1/driving/...`
- Base: `http://localhost:8080/osrm/base/route/v1/driving/...`

See `nginx.conf` for routing configuration.
