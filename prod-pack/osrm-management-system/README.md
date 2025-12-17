# OSRM Management System - Production

This directory contains the built OSRM Management System service.

## Usage

1. Install dependencies:
```bash
npm install --production
```

2. Generate Prisma client:
```bash
npx prisma generate
```

3. Configure environment (service uses .env directly):
```bash
cp env.local .env
# Edit .env with your settings
```

4. Start the service:
```bash
node dist/index.js
```

## API Endpoints

- `GET /api/v1/health` - Health check
- `POST /api/v1/extract/complete` - Extract complete OSM data
- `POST /api/v1/generate/osrm-v2` - Generate OSRM V2 models

## Notes

- Service runs on port 21520 by default
- All paths are relative to the working directory (wwwroot/dss/)
- Requires database connection to zone_service database
- Requires osmium-tool installed on the system
- Requires Docker for OSRM processing
