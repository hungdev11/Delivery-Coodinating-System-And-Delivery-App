# Zone Service Data Setup Guide

This guide explains how to set up the required data files for the Zone Service, which are needed for Docker Compose to work properly.

## Directory Structure

The following directories are required by the application but their contents are ignored by git due to size:

```
BE/zone_service/
├── osrm_data/                    # OSRM routing engine data
│   ├── osrm-instance-1/         # Primary OSRM instance data
│   ├── osrm-instance-2/         # Secondary OSRM instance data
│   └── test-instance/           # Test OSRM instance data
├── raw_data/                    # Raw OpenStreetMap data
│   ├── new_hochiminh_city/      # HCMC district data
│   ├── old_thuduc_city/         # Old Thu Duc city data
│   └── vietnam/                 # Vietnam country data
├── processors/                  # Data processing outputs
└── parsers/                     # Parser outputs
```

## Docker Compose Requirements

The `docker-compose.yml` file mounts these directories as volumes for the OSRM instances:

```yaml
volumes:
  - ./BE/zone_service/osrm_data/osrm-instance-1:/data:ro
  - ./BE/zone_service/osrm_data/osrm-instance-2:/data:ro
```

## Required Data Files

### OSRM Data Files

Each OSRM instance directory needs these files:

```
osrm-instance-1/
├── custom_car.lua              # Custom routing profile
├── network.osm.xml             # OSM XML data
├── network.osrm                # Main OSRM data file
├── network.osrm.cell_metrics   # Cell metrics
├── network.osrm.cells          # Cell data
├── network.osrm.cnbg           # CNBG data
├── network.osrm.cnbg_to_ebg    # CNBG to EBG mapping
├── network.osrm.datasource_names # Data source names
├── network.osrm.ebg            # EBG data
├── network.osrm.ebg_nodes      # EBG nodes
├── network.osrm.edges          # Edge data
├── network.osrm.enw            # ENW data
├── network.osrm.fileIndex      # File index
├── network.osrm.geometry       # Geometry data
├── network.osrm.icd            # ICD data
├── network.osrm.maneuver_overrides # Maneuver overrides
├── network.osrm.mldgr          # MLD graph
├── network.osrm.names          # Name data
├── network.osrm.nbg_nodes      # NBG nodes
├── network.osrm.partition      # Partition data
├── network.osrm.properties     # Properties
├── network.osrm.ramIndex       # RAM index
├── network.osrm.restrictions   # Restrictions
├── network.osrm.timestamp      # Timestamp
├── network.osrm.tld            # TLD data
├── network.osrm.tls            # TLS data
├── network.osrm.turn_duration_penalties # Turn duration penalties
├── network.osrm.turn_penalties_index # Turn penalties index
└── network.osrm.turn_weight_penalties # Turn weight penalties
```

### Raw Data Files

The `raw_data` directory should contain:

```
raw_data/
├── new_hochiminh_city/
│   ├── hochiminh_city.osm.pbf  # Full HCMC OSM data
│   ├── hochiminh_city.poly     # HCMC boundary
│   ├── donghoa_district.poly   # District boundaries
│   ├── linhxuan_district.poly
│   ├── longbinh_district.poly
│   ├── longphuoc_district.poly
│   ├── longtruong_district.poly
│   ├── phuoclong_district.poly
│   ├── tangnhonphu_district.poly
│   └── thuduc_ward.poly
├── old_thuduc_city/
│   └── hcmc.poly          # Old Thu Duc boundary
└── vietnam/
    └── vietnam-250925.osm.pbf  # Vietnam OSM data
```

## Data Generation

### 1. Generate OSRM Data

Use the provided scripts to generate OSRM data:

```bash
# Generate OSRM data for instance 1
npm run generate:osrm-instance-1

# Generate OSRM data for instance 2  
npm run generate:osrm-instance-2

# Or generate all instances
npm run generate:osrm-all
```

### 2. Download Raw Data

Download the required OSM data files:

```bash
# Download HCMC data
wget https://download.geofabrik.de/asia/vietnam/hochiminh-latest.osm.pbf -O raw_data/new_hochiminh_city/hochiminh_city.osm.pbf

# Download Vietnam data
wget https://download.geofabrik.de/asia/vietnam-latest.osm.pbf -O raw_data/vietnam/vietnam-250925.osm.pbf
```

### 3. Extract District Boundaries

Use osmium-tool to extract district boundaries:

```bash
# Extract Thu Duc districts
osmium extract --polygon=raw_data/new_hochiminh_city/thuduc_ward.poly raw_data/new_hochiminh_city/hochiminh_city.osm.pbf -o raw_data/new_hochiminh_city/thuduc_ward.osm.pbf
```

## Git Configuration

The `.gitignore` file is configured to:

- ✅ **Track**: Directory structure via `.gitkeep` files
- ❌ **Ignore**: All large binary data files (`.osrm`, `.osm.pbf`, `.poly`, etc.)
- ❌ **Ignore**: Processed data outputs

This ensures:
- Docker Compose can mount the required directories
- Repository stays lightweight
- Team members can clone without downloading large data files
- Directory structure is preserved for development

## Troubleshooting

### Issue: "Directory not found" in Docker Compose

Ensure all required directories exist with `.gitkeep` files:

```bash
# Check if directories exist
ls -la BE/zone_service/osrm_data/
ls -la BE/zone_service/raw_data/

# If missing, the .gitkeep files should create them
git add BE/zone_service/*/.gitkeep
```

### Issue: OSRM instances fail to start

Check that OSRM data files exist in the mounted directories:

```bash
# Check OSRM instance 1 data
ls -la BE/zone_service/osrm_data/osrm-instance-1/

# Check OSRM instance 2 data  
ls -la BE/zone_service/osrm_data/osrm-instance-2/
```

### Issue: Large repository size

Verify that large files are properly ignored:

```bash
# Check git status
git status

# Verify .gitignore is working
git check-ignore BE/zone_service/osrm_data/osrm-instance-1/network.osrm
```

## Next Steps

After setting up the data:

1. **Run Docker Compose**: `docker-compose up -d`
2. **Verify OSRM Health**: Check that both OSRM instances are healthy
3. **Test Routing**: Use the zone service to test routing functionality
4. **Monitor Logs**: Check logs for any data-related issues

## Data Sources

- **OpenStreetMap**: [Geofabrik Downloads](https://download.geofabrik.de/)
- **OSRM**: [Project OSRM](http://project-osrm.org/)
- **Osmium Tool**: [Osmium Documentation](https://osmcode.org/osmium-tool/)
