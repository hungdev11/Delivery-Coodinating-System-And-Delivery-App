# Raw Data Directory

This directory contains raw data files used by the OSRM service.

## Structure

```
raw_data/
├── vietnam/           # Vietnam OSM data
│   └── vietnam-*.osm.pbf  # OSM files (auto-detected by service)
├── poly/              # Polygon data (if any)
└── README.md          # This file
```

## OSM Files

- **Pattern**: `vietnam-YYYYMMDD.osm.pbf`
- **Auto-Detection**: Service automatically finds the latest file
- **Environment**: Set `OSM_RAW_DATA_PATH` to change directory

## Usage

The OSRM service will:
1. Scan this directory for OSM files
2. Select the latest file by modification date
3. Use it for building OSRM routing data

## Adding New OSM Files

1. Place new `.osm.pbf` files in the `vietnam/` directory
2. Use naming pattern: `vietnam-YYYYMMDD.osm.pbf`
3. The service will automatically detect and use the latest file
4. Or call `/api/v1/osrm/refresh-osm` to refresh detection
