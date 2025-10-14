# Zone Service Database Seeding

This directory contains scripts to seed the zone service database with geographic and road network data from OpenStreetMap using **osmium-tool**.

## Overview

The seeding process uses osmium-tool for efficient OSM data extraction:

1. **Zones Seeding** - Process district polygon files and populate zones table
2. **Roads Seeding** - Parse OSM PBF files and extract road network data with:
   - Filtering by Thu Duc boundary
   - Duplicate road name detection and merging
   - Intersection detection and road segment generation
   - Base weight calculation for routing

## Prerequisites

### 1. Install osmium-tool

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install osmium-tool
```

**macOS:**
```bash
brew install osmium-tool
```

**Windows:**
Use WSL (Windows Subsystem for Linux) and install via apt-get.

Verify installation:
```bash
osmium --version
```

### 2. Database Setup

Ensure PostgreSQL is running and connection string is set:
```bash
export ZONE_DB_CONNECTION="postgresql://user:password@localhost:5432/zone_db"
# Or set DATABASE_URL
```

### 3. Install Node Dependencies

```bash
cd prisma/seeds
npm install
```

### 4. Run Database Migrations

```bash
cd ../.. # Back to zone_service root
npm run prisma:migrate
```

## Quick Start

Check if everything is ready:
```bash
npm run check:osmium
```

Then run the seeding process:
```bash
# Step 1: Seed zones (district boundaries)
npm run seed:zones

# Step 2: Seed roads (road network)
npm run seed:roads

# Or run all seeders
npm run seed
```

## Available Commands

### Extraction & Analysis
```bash
npm run extract:thuduc    # Extract Thu Duc roads and analyze
```

### Database Seeding
```bash
npm run seed:zones        # Seed district zones (Step 1)
npm run seed:roads        # Seed roads, nodes, segments (Step 2)
npm run seed              # Run all seeders
```

### Utilities
```bash
npm run check:osmium      # Check installation
npm run help              # Show help
```

### Prisma Commands
```bash
cd ../.. && npm run prisma:studio   # View data
cd ../.. && npm run prisma:migrate  # Run migrations
```

## Data Sources

### Zone Polygons
- Location: `./raw_data/new_hochiminh_city/*.poly`
- Format: OSM Poly format
- Content: District boundaries for Thu Duc districts

### Road Network
- Location: `./raw_data/new_hochiminh_city/hochiminh_city.osm.pbf`
- Format: OSM PBF (Protobuf Binary Format)
- Content: Full road network for Ho Chi Minh City
- Filter: Old Thu Duc city boundary (`./raw_data/old_thuduc_city/thuduc_cu.poly`)

## Features

### Osmium Integration
- Fast PBF parsing using osmium-tool
- Polygon boundary filtering
- Tag-based road filtering (highway=*)
- GeoJSON export for processing

### Duplicate Road Handling
The seeder intelligently merges roads with duplicate names (e.g., "Xa lộ Hà Nội" spanning multiple districts):
- Detects roads with same name
- Checks if segments are connected (within 50m)
- Merges connected segments into single roads
- Preserves unconnected segments as separate roads

### Intersection Detection
- Identifies where multiple roads meet
- Creates nodes for intersections and waypoints
- Distinguishes between intersections (3+ roads) and waypoints (2 roads or endpoints)

### Road Segments
Each segment includes:
- From/to nodes
- Geometry (GeoJSON LineString)
- Length in meters
- Road attributes (name, type, speed, lanes)
- **Base weight** - Static routing cost based on length, speed, road type
- **Delta weight** - Dynamic adjustment (traffic, user feedback)
- **Current weight** - Total routing cost (base + delta)

### Weight Calculation

**Base Weight** = f(length, speed, road_type, lanes)
- Time to traverse (length / speed)
- Road type multiplier (motorway = 1.0, path = 3.0)
- Lane multiplier (more lanes = lower weight)

**Delta Weight** = f(traffic, user_feedback)
- Traffic multiplier (from tracking-asia)
- User feedback adjustments

**Current Weight** = Base Weight + Delta Weight

## District Mapping

The following districts within old Thu Duc are processed:

| Code | Name            | Poly File                  |
|------|-----------------|----------------------------|
| TD   | Thủ Đức         | thuduc_ward.poly          |
| LX   | Linh Xuân       | linhxuan_district.poly    |
| TNP  | Tăng Nhơn Phú   | tangnhonphu_district.poly |
| LB   | Long Bình       | longbinh_district.poly    |
| LP   | Long Phước      | longphuoc_district.poly   |
| LT   | Long Trường     | longtruong_district.poly  |
| PL   | Phước Long      | phuoclong_district.poly   |
| DH   | Đông Hòa        | donghoa_district.poly     |

## Road Types

Supported OSM highway types:

- `MOTORWAY` - Expressways/highways (cao tốc)
- `TRUNK` - Major arterial roads
- `PRIMARY` - Primary roads
- `SECONDARY` - Secondary roads
- `TERTIARY` - Tertiary roads
- `RESIDENTIAL` - Residential streets
- `SERVICE` - Service roads
- `UNCLASSIFIED` - Unclassified roads
- `LIVING_STREET` - Living streets
- `PEDESTRIAN` - Pedestrian zones
- `TRACK` - Tracks
- `PATH` - Paths

## Troubleshooting

### Issue: "Cannot find module '@prisma/client'"
```bash
cd ../..  # Back to zone_service root
npm run prisma:generate
cd prisma/seeds
```

### Issue: "OSM PBF parsing not implemented"
The current implementation is a framework. To actually parse PBF files, you need to:

1. Install osmium-tool
2. Implement PBF parsing in `osm-parser.ts` using osmium bindings
3. Or use an alternative like osm2pgsql

### Issue: "Database connection failed"
Ensure:
1. PostgreSQL is running
2. ZONE_DB_CONNECTION environment variable is set
3. Database migrations have been run (`npm run prisma:migrate`)

## Next Steps

After seeding:

1. **Create Spatial Indexes**
   ```sql
   CREATE INDEX idx_roads_geometry ON roads USING GIST (geometry);
   CREATE INDEX idx_segments_geometry ON road_segments USING GIST (geometry);
   ```

2. **Verify Data**
   ```bash
   npm run prisma:studio
   ```

3. **Generate OSRM Data**
   - Use the seeded data to generate custom OSRM routing data
   - See `../services/osrm-generator/` (to be implemented)

4. **Set Up Traffic Integration**
   - Implement tracking-asia traffic data fetching
   - See `../services/traffic-updater/` (to be implemented)

## References

- [OSM Wiki - Poly Format](https://wiki.openstreetmap.org/wiki/Osmosis/Polygon_Filter_File_Format)
- [OSM PBF Format](https://wiki.openstreetmap.org/wiki/PBF_Format)
- [Osmium Tool](https://osmcode.org/osmium-tool/)
- [OSRM](http://project-osrm.org/)
