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
# IMPORTANT: .env file must be in osrm-management-system/ directory (same level as dist/)
cp env.local .env
# Edit .env with your actual database credentials and paths
# Example:
# DB_HOST=127.0.0.1
# DB_PORT=3306
# DB_USERNAME=your_username
# DB_PASSWORD=your_password
# ZONE_DB_NAME=ds_zone_service
# RAW_DATA_PATH=./raw_data
# OSRM_DATA_PATH=./osrm_data
```

4. Start the service:
```bash
# Make sure you're in osrm-management-system/ directory
cd /www/wwwroot/dss/osrm-management-system
node dist/index.js
```

**Important Notes:**
- The `.env` file must be in the `osrm-management-system/` directory (same level as `dist/`, `package.json`)
- The service will look for `.env` at `osrm-management-system/.env` (parent of `dist/`)
- If `.env` is not found, it will fallback to current working directory
- Make sure database credentials in `.env` are correct for your production environment

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
