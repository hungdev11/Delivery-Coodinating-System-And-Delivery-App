# OSRM Management System

Independent service for managing OSRM data extraction and generation.

## Features

- **OSM Data Extraction**: Extract complete OSM data (routing + addresses) from PBF files
- **OSRM Data Generation**: Generate OSRM V2 models from database
- **RESTful API**: Standard RESTful API endpoints following RESTFUL.md
- **Independent Service**: Fully independent, can run standalone

## Installation

```bash
npm install
npm run prisma:generate
```

## Quick Start

```bash
# 1. Copy env file
cp env.local .env

# 2. Install dependencies
npm install

# 3. Generate Prisma client (required after schema changes)
npm run prisma:generate

# 4. Start development server
npm run dev
```

Server will run on `http://localhost:21520`

## Configuration

Edit `.env` file to configure:
- Database connection (DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD, ZONE_DB_NAME)
- Paths (RAW_DATA_PATH, OSRM_DATA_PATH)
- Server port (PORT)

The service uses `.env` file directly via `dotenv`.

## Development

```bash
npm run dev
```

See [DEV_TESTING.md](./DEV_TESTING.md) for detailed testing guide.

Quick test commands:
```bash
# Health check
npm run test:health

# Test extract (requires osmium-tool and data)
npm run test:extract

# Test generate (requires database data and Docker)
npm run test:generate
```

## Build

```bash
npm run build
```

## Production

```bash
npm run build
npm start
```

## API Endpoints

### Health Check
- `GET /api/v1/health` - Health check endpoint

### Extract
- `POST /api/v1/extract/complete` - Extract complete OSM data
  ```json
  {
    "polyFile": "./raw_data/poly/thuduc_cu.poly" // optional
  }
  ```

### Generate
- `POST /api/v1/generate/osrm-v2` - Generate all OSRM V2 models from database

## Environment Variables

See `env.local` for all available environment variables.
