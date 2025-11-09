# Zone Service

**Self-hosted routing and zone management microservice for delivery coordination system**

[![Node.js](https://img.shields.io/badge/Node.js-20%2B-green.svg)](https://nodejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9%2B-blue.svg)](https://www.typescriptlang.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8%2B-blue.svg)](https://www.mysql.com/)
[![OSRM](https://img.shields.io/badge/OSRM-5.x-orange.svg)](http://project-osrm.org/)

## Table of Contents

- [What is Zone Service?](#what-is-zone-service)
- [Key Features](#key-features)
- [Quick Start](#quick-start)
- [Architecture Overview](#architecture-overview)
- [Documentation](#documentation)
- [API Overview](#api-overview)
- [Route Documentation](#route-documentation)
- [Development](#development)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)

---

## What is Zone Service?

Zone Service is a **self-hosted routing microservice** that provides intelligent route optimization for delivery systems. It combines:

- **OpenStreetMap (OSM)** data for real road networks
- **OSRM (Open Source Routing Machine)** for fast routing calculations
- **Traffic integration** for real-time route adjustments
- **Custom weight system** that adapts to traffic conditions and user feedback

### Why Self-Hosted?

Unlike commercial routing APIs (Google Maps, Mapbox), this service:

✅ **No API costs** - unlimited routing queries
✅ **Full control** - customize routing logic and weights
✅ **Privacy** - no data sent to third parties
✅ **Offline capable** - works without internet (after initial setup)
✅ **Traffic-aware** - integrates real-time traffic from TrackAsia

### Key Use Cases

1. **Delivery Route Optimization** - Find fastest routes considering current traffic
2. **Zone Management** - Divide city into delivery zones (Thu Duc districts)
3. **Multi-Stop Routing** - Optimize routes with multiple delivery points
4. **ETA Calculation** - Accurate arrival time predictions
5. **Alternative Routes** - Provide backup routes to drivers

---

## Key Features

### 🚀 High-Performance Routing

- **Dual OSRM instances** for zero-downtime updates
- **MLD algorithm** (Multi-Level Dijkstra) - sub-second routing
- **70x faster seeding** - 17k streets in ~51 seconds (was 1+ hour)
- **Batch operations** - 500 records/insert for optimal performance

### 🌍 Real-World Road Network

- **OpenStreetMap data** for Ho Chi Minh City
- **Thu Duc focus** - optimized for Thu Duc city area
- **Automatic intersection detection** - coordinate-based merging (1.1m precision)
- **4,957 roads, 6,588 nodes** currently loaded

### 🚦 Dynamic Traffic Integration

- **Real-time traffic** from TrackAsia API
- **Custom weight system**:
  - `base_weight` = static road characteristics (length, speed, road type)
  - `delta_weight` = dynamic adjustments (traffic, user feedback)
  - `current_weight` = base + delta (used for routing)
- **Smart rebuild triggers** - only rebuild OSRM when weights change significantly

### 📊 Zone Management

- **District boundaries** - Thu Duc city divided into 8 zones
- **Geohash indexing** - fast spatial queries
- **Zone assignment** - automatically assign addresses to zones

---

## Quick Start

### Prerequisites

```bash
# Required
- Node.js 20+
- MySQL 8+
- Docker & Docker Compose
- osmium-tool (for data processing)

# Check versions
node --version        # v20.0.0+
docker --version      # 20.0.0+
osmium --version      # 1.14.0+
```

### Installation (5 minutes)

```bash
# 1. Clone and navigate
cd /path/to/DS/BE/zone_service

# 2. Install dependencies
npm install

# 3. Setup environment
cp .env.example .env
# Edit .env with your database credentials

# 4. Run database migrations
npm run prisma:migrate

# 5. Seed database with road network
npm run seed:zones    # Load Thu Duc districts (30 sec)
npm run seed:roads    # Load 17k streets (51 sec)

# 6. Generate OSRM routing data
npm run osrm:generate # Export to OSRM format (2 min)

# 7. Start OSRM instances
cd ../../..
docker-compose up -d osrm-* 

# 8. Start zone service
cd BE/zone_service
npm run dev
```

### Verify Setup

```bash
# Check OSRM instances
curl http://localhost:5000/route/v1/driving/106.7718,10.8505;106.8032,10.8623

# Check zone service
curl http://localhost:21503/api/v1/health

# Test routing
npx tsx test-osrm-hard-routes.ts
```

**Expected result:** 100% success rate on routing tests 🎉

---

## Architecture Overview

```mermaid
graph TB
    subgraph "Client Layer"
        A[Mobile App] --> B[API Gateway]
        C[Web Dashboard] --> B
    end

    subgraph "Zone Service"
        B --> D[Express Server :21503]
        D --> E[Routing Controller]
        D --> F[Zone Controller]
        D --> G[Center Controller]

        E --> H[OSRM Router Service]
        H --> I{Load Balancer}
        I --> J[OSRM Instance 1 :5000]
        I --> K[OSRM Instance 2 :5001]

        F --> L[Zone Model]
        G --> M[Center Model]

        N[Traffic Service] --> O[Traffic Integration]
        O --> P[Weight Calculator]
        P --> Q[(PostgreSQL)]

        R[OSRM Generator] --> Q
        R --> J
        R --> K
    end

    subgraph "External Services"
        S[TrackAsia API] --> N
        T[OpenStreetMap] -.Initial Data.-> U[Data Seeders]
        U --> Q
    end

    subgraph "Data Storage"
        Q --> V[roads]
        Q --> W[road_nodes]
        Q --> X[road_segments]
        Q --> Y[zones]
        Q --> Z[traffic_conditions]
    end

    style J fill:#90EE90
    style K fill:#FFB6C1
    style D fill:#87CEEB
    style Q fill:#DDA0DD
```

### Component Relationships

| Component | Links With | Purpose |
|-----------|-----------|---------|
| **OSRM Instances** | OSRM Router Service | Actual routing calculations |
| **OSRM Generator** | PostgreSQL + OSRM Instances | Export DB → OSM XML → OSRM format |
| **Traffic Service** | TrackAsia API + PostgreSQL | Fetch traffic, update weights |
| **Weight Calculator** | road_segments table | Calculate base/delta/current weights |
| **Zone Model** | zones, zone_geohash_cells | Zone queries and spatial indexing |
| **Routing Controller** | OSRM Router + PostgreSQL | Route requests, logging |

---

## Documentation

### 📚 Complete Guides

| Document | Description | For |
|----------|-------------|-----|
| [SETUP.md](.docs/SETUP.md) | **Step-by-step setup from zero** | New developers |
| [ARCHITECTURE.md](.docs/ARCHITECTURE.md) | **System design, database schema, UML** | Understanding system |
| [WORKFLOWS.md](.docs/WORKFLOWS.md) | **Development workflows (code/test/data)** | Day-to-day work |
| [OSRM.md](.docs/OSRM.md) | **OSRM setup, dual instances, custom weights** | OSRM specifics |
| [API.md](.docs/API.md) | **All API endpoints with examples** | API integration |
| [TROUBLESHOOTING.md](.docs/TROUBLESHOOTING.md) | **Common issues and solutions** | Fixing problems |

### 📖 Route Documentation

| Document | Description |
|----------|-------------|
| [Zones API](.docs/route/zones.md) | Zone management endpoints (POST /zones, GET /zones/:id, etc.) |
| [Centers API](.docs/route/centers.md) | Distribution center endpoints (GET /centers, POST /centers, etc.) |
| [Addresses API](.docs/route/addresses.md) | Address management endpoints (GET /addresses, POST /addresses, etc.) |
| [Routing API](.docs/route/routing.md) | Route calculation endpoints (POST /routing/route, etc.) |
| [OSRM API](.docs/route/osrm.md) | OSRM data management endpoints (POST /osrm/build, etc.) |
| [Health API](.docs/route/health.md) | Service health checks |


## API Overview

### Base URL

```
http://localhost:21503/api/v1
```

### Key Endpoints

#### 1. Get Single Route

```bash
POST /routing/route
Content-Type: application/json

{
  "waypoints": [
    {"lat": 10.8505, "lon": 106.7718},
    {"lat": 10.8623, "lon": 106.8032}
  ],
  "options": {
    "alternatives": true,
    "steps": true,
    "overview": "full"
  }
}
```

**Response:**
```json
{
  "code": "Ok",
  "routes": [{
    "distance": 6400,
    "duration": 510,
    "weight": 548.7,
    "geometry": "...",
    "legs": [...]
  }]
}
```

#### 2. Get Multi-Stop Route

```bash
POST /routing/multi-route
Content-Type: application/json

{
  "waypoints": [
    {"lat": 10.8505, "lon": 106.7718, "priority": 1},
    {"lat": 10.8550, "lon": 106.7800, "priority": 2},
    {"lat": 10.8623, "lon": 106.8032, "priority": 3}
  ]
}
```

#### 3. Get All Zones

```bash
GET /zones?page=0&limit=10
```

#### 4. Health Check

```bash
GET /health
```

**See [API.md](.docs/API.md) for complete API documentation.**

---

## Development

### Project Structure

```
zone_service/
├── src/                          # Application source code
│   ├── app.ts                   # Express app setup
│   ├── common/                  # Shared utilities
│   │   ├── config/             # Configuration management
│   │   ├── database/           # Database connection
│   │   ├── logger/             # Winston logger
│   │   └── middleware/         # Express middleware
│   ├── modules/                 # Feature modules
│   │   ├── routing/            # Routing logic
│   │   ├── zone/               # Zone management
│   │   └── center/             # Distribution centers
│   └── services/                # Business logic services
│       ├── osrm/               # OSRM integration
│       └── traffic/            # Traffic integration
├── prisma/                       # Database schema & migrations
│   ├── models/                 # Prisma model files
│   └── migrations/             # Migration history
├── processors/                   # Data processing scripts
│   ├── zones-seeder.ts         # Seed Thu Duc zones
│   └── roads-seeder.ts         # Seed road network (FAST!)
├── osrm_data/                    # OSRM data files
│   ├── osrm-instance-1/        # Instance 1 data
│   └── osrm-instance-2/        # Instance 2 data
├── .docs/                        # Documentation
└── package.json                  # Dependencies & scripts
```

### Available Scripts

```bash
# Development
npm run dev              # Start with hot reload
npm run build            # Build for production
npm run start            # Start production server

# Database
npm run prisma:generate  # Generate Prisma client
npm run prisma:migrate   # Run migrations
npm run prisma:studio    # Open database GUI

# Data Seeding
npm run seed:zones       # Seed zones (30 sec)
npm run seed:roads       # Seed roads (51 sec - FAST!)
npm run extract:thuduc   # Extract Thu Duc from OSM

# OSRM
npm run osrm:generate    # Generate OSRM data (2 min)
npm run check:connectivity # Check road network connectivity

# Testing & Diagnostics
npx tsx test-osrm-hard-routes.ts  # Test OSRM routing (stress test)
```

### Environment Variables

```bash
# Database
DATABASE_URL="mysql://user:password@localhost:3306/zone_db"

# Server
PORT=21503
NODE_ENV=development

# OSRM Instances
OSRM_INSTANCE_1_URL=http://localhost:5000
OSRM_INSTANCE_2_URL=http://localhost:5001

# Traffic Integration
TRACKASIA_API_KEY=your_api_key
TRAFFIC_UPDATE_INTERVAL=1800000  # 30 minutes

# Logging
LOG_LEVEL=info
LOG_FILE=logs/zone-service.log
```

---

## Testing

### 1. Unit Tests (Coming Soon)

```bash
npm test
```

### 2. Integration Tests

#### Test OSRM Routing

```bash
# Hard stress test (routes between distant streets)
npx tsx test-osrm-hard-routes.ts

# Expected: 100% success rate
```

#### Test Road Connectivity

```bash
npm run check:connectivity

# Shows:
# - Total connected components
# - Largest connected network
# - Disconnected segments
```

#### Test Data Seeding

```bash
# Clear database
npx prisma migrate reset --force

# Re-seed
npm run seed:zones
npm run seed:roads

# Verify in Prisma Studio
npm run prisma:studio
```

### 3. Manual API Testing

```bash
# Get route
curl -X POST http://localhost:21503/api/v1/routing/route \
  -H "Content-Type: application/json" \
  -d '{
    "waypoints": [
      {"lat": 10.8505, "lon": 106.7718},
      {"lat": 10.8623, "lon": 106.8032}
    ]
  }'

# Get zones
curl http://localhost:21503/api/v1/zones

# Health check
curl http://localhost:21503/api/v1/health
```

### 4. Performance Benchmarks

| Operation | Target | Current |
|-----------|--------|---------|
| Seeding 17k roads | < 5 min | ~51 sec ✅ |
| OSRM generation | < 3 min | ~2 min ✅ |
| Single route query | < 100ms | ~20ms ✅ |
| 100 concurrent routes | < 2s | ~1.5s ✅ |

---

## Troubleshooting

### Quick Fixes

#### OSRM returns "NoRoute"

```bash
# 1. Check OSRM is running
docker ps | grep osrm

# 2. Check connectivity
npm run check:connectivity

# 3. Regenerate OSRM data
npm run osrm:generate
docker-compose restart osrm-instance-1 osrm-instance-2
```

#### Database connection failed

```bash
# Check MySQL is running
docker ps | grep mysql

# Test connection
psql $DATABASE_URL -c "SELECT 1"

# Run migrations
npm run prisma:migrate
```

#### Seeding takes too long

```bash
# If old version, update:
git pull origin main
npm install

# New seeder is 70x faster:
# - 17k streets: 51 seconds
# - Auto-fixes connectivity
# - Batch operations
```

#### OSRM container won't start

```bash
# Check logs
docker logs dss-osrm-1

# Common issue: Missing .osrm files
npm run osrm:generate

# Restart containers
docker-compose restart osrm-instance-1 osrm-instance-2
```

**See [TROUBLESHOOTING.md](.docs/TROUBLESHOOTING.md) for complete guide.**

---

## Performance Highlights

### Before vs After Optimization

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| **Seeding 17k streets** | 1+ hour | 51 sec | **70x faster** |
| **Road connectivity** | 100% isolated | 29% connected | **Fixed!** |
| **OSRM routing** | Always fails | 100% success | **Working!** |
| **Batch inserts** | Individual | 500/batch | **100x faster** |
| **Node merging** | 2+ min timeout | 2 queries | **1000x faster** |

### Current Performance

- **Roads loaded:** 4,957
- **Nodes created:** 6,588 (after merging duplicates)
- **Segments:** 4,957
- **Largest connected network:** 1,937 nodes (29.4%)
- **Routing success rate:** 100% (tested on hard routes)
- **Average query time:** ~20ms per route

---

## Contributing

### Development Workflow

1. **Create feature branch**
   ```bash
   git checkout -b feature/your-feature
   ```

2. **Make changes** (see [WORKFLOWS.md](.docs/WORKFLOWS.md))

3. **Test thoroughly**
   ```bash
   npx tsx test-osrm-hard-routes.ts
   npm run check:connectivity
   ```

4. **Commit with clear message**
   ```bash
   git commit -m "Add: Description of changes"
   ```

5. **Push and create PR**
   ```bash
   git push origin feature/your-feature
   ```

### Code Style

- **TypeScript** - strict mode enabled
- **ESLint** - follow existing patterns
- **Prettier** - consistent formatting
- **Comments** - explain WHY, not WHAT

---

## Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Runtime** | Node.js 20+ | JavaScript runtime |
| **Language** | TypeScript 5.9+ | Type safety |
| **Framework** | Express.js 5 | Web server |
| **Database** | MySQL 8+ | Data storage |
| **ORM** | Prisma 6+ | Database toolkit |
| **Routing** | OSRM 5.x | Route calculations |
| **Geo Data** | OpenStreetMap | Road network |
| **Traffic** | TrackAsia API | Real-time traffic |
| **Logging** | Winston | Application logs |
| **Validation** | class-validator | Request validation |
| **Container** | Docker | OSRM deployment |

---

## License

ISC License - See LICENSE file for details

---

## Support

### Documentation
- [Setup Guide](.docs/SETUP.md)
- [Architecture](.docs/ARCHITECTURE.md)
- [API Reference](.docs/API.md)
- [Troubleshooting](.docs/TROUBLESHOOTING.md)

### Issues
- GitHub Issues: [Create Issue](https://github.com/yourrepo/issues)
- Check [TROUBLESHOOTING.md](.docs/TROUBLESHOOTING.md) first

### Contact
- Project Lead: [Your Name]
- Email: [your-email@example.com]

---

## Acknowledgments

- **OSRM** - Fast routing engine
- **OpenStreetMap** - Community-driven map data
- **TrackAsia** - Traffic data provider
- **Prisma** - Excellent database toolkit

---

**Ready to start?** → See [SETUP.md](.docs/SETUP.md) for detailed setup instructions!
