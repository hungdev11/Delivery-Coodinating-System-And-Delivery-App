# OSRM Build and Management Guide

## ⚠️ Important: Manual Build Only

**OSRM data builds are NOT automatic.** You must manually trigger builds through the **osrm-management-system** service.

## Overview

The OSRM system uses 4 different models:
- **osrm-full**: Rating affects weight, Blocking affects speed
- **osrm-rating-only**: User feedback affects weight only
- **osrm-blocking-only**: Traffic affects speed only
- **osrm-base**: VN motorbike optimized, no modifiers

## How to Build OSRM Data

### Option 1: Via API (Recommended)

Use the osrm-management-system API:

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

## Prerequisites

### Install osmium-tool (Required for OSM extraction)

**On Ubuntu 22.04 (Manual Installation - Recommended):**

If `apt-get install osmium-tool` doesn't work or installs an incompatible version, use manual installation:

```bash
# Download and install boost dependency
wget http://archive.ubuntu.com/ubuntu/pool/main/b/boost1.65.1/libboost-program-options1.65.1_1.65.1+dfsg-0ubuntu5_amd64.deb
sudo dpkg -i libboost-program-options1.65.1_1.65.1+dfsg-0ubuntu5_amd64.deb

# Download and install osmium-tool 1.7.1
wget http://launchpadlibrarian.net/344172654/osmium-tool_1.7.1-1_amd64.deb
sudo dpkg -i osmium-tool_1.7.1-1_amd64.deb

# Verify installation
osmium --version  # Should show 1.7.1
```

**On Linux (Ubuntu/Debian - Standard Installation):**
```bash
sudo apt-get update
sudo apt-get install osmium-tool

# Verify installation
osmium --version  # Should show 1.14+ (or 1.18+ for modern format)
```

**On macOS:**
```bash
brew install osmium-tool

# Verify installation
osmium --version
```

**Note:** 
- `osmium-tool` is required only if you need to extract OSM data from PBF files. If you already have extracted OSM data, you can skip this step.
- The code automatically detects osmium version and uses the appropriate command format:
  - Version >= 1.18: Modern format with `-s complete_ways --overwrite input -o output --polygon poly`
  - Version < 1.18: Legacy format with `-p poly -s complete_ways -O -o output input`

## Build Process

1. **Extract OSM Data** (if needed):
   - Extracts routing graph and addresses from Vietnam PBF file
   - Uses polygon file to clip to specific area
   - Output: `raw_data/extracted/thuduc_complete.osm.pbf`

2. **Generate OSRM Models**:
   - Reads road network data from database
   - Generates 4 OSRM models
   - Builds are tracked in `osrm_builds` database table
   - Status: PENDING → BUILDING → READY → DEPLOYED

3. **Deploy** (after build completes):
   - Rebuild containers to use new data:
   ```bash
   docker-compose -p dss -f docker-compose.yml restart osrm-v2-full
   docker-compose -p dss -f docker-compose.yml restart osrm-v2-rating-only
   docker-compose -p dss -f docker-compose.yml restart osrm-v2-blocking-only
   docker-compose -p dss -f docker-compose.yml restart osrm-v2-base
   ```

## Container Management

Use osrm-management-system API for container operations:

```bash
# Get container status
curl http://localhost:21520/api/v1/osrm/containers/status

# Start container
curl -X POST http://localhost:21520/api/v1/osrm/containers/osrm-full/start

# Restart container
curl -X POST http://localhost:21520/api/v1/osrm/containers/osrm-full/restart

# Rebuild container (after new data)
curl -X POST http://localhost:21520/api/v1/osrm/containers/osrm-full/rebuild
```

## Notes

- **No automatic builds** on container startup
- Builds run **sequentially** (one at a time per model)
- Build status is tracked in database (`osrm_builds` table)
- All operations are manual via API or UI

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

### osmium-tool not found or incompatible version

**Symptoms:**
```
bash: osmium: command not found
# Or
Command failed: osmium extract ...
```

**Solution for Ubuntu 22.04:**

```bash
# Manual installation with specific version (1.7.1)
wget http://archive.ubuntu.com/ubuntu/pool/main/b/boost1.65.1/libboost-program-options1.65.1_1.65.1+dfsg-0ubuntu5_amd64.deb
sudo dpkg -i libboost-program-options1.65.1_1.65.1+dfsg-0ubuntu5_amd64.deb

wget http://launchpadlibrarian.net/344172654/osmium-tool_1.7.1-1_amd64.deb
sudo dpkg -i osmium-tool_1.7.1-1_amd64.deb

# Verify
osmium --version  # Should show 1.7.1
```

**Solution for other Ubuntu/Debian versions:**

```bash
sudo apt-get update
sudo apt-get install -y osmium-tool
osmium --version
```

**Note:** The code automatically detects osmium version and uses the appropriate command format:
- Version >= 1.18: Modern format
- Version < 1.18: Legacy format (auto-detected)

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
   # Copy pre-built data manually to osrm_data/ directory
   ```

3. **Option 3**: Start zone-service container first
   ```bash
   docker-compose up -d zone-service
   # Wait for it to be healthy
   # Generate OSRM data (skip extract if already done)
curl -X POST http://localhost:21520/api/v1/generate/osrm-v2
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
