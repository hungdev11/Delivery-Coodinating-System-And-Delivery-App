# Setup Guide - Zone Service

**Complete step-by-step guide to set up Zone Service from zero**

This guide will help you set up the Zone Service on a fresh machine. Estimated time: **30-45 minutes**.

---

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Install Prerequisites](#install-prerequisites)
3. [Database Setup](#database-setup)
4. [Zone Service Setup](#zone-service-setup)
5. [Data Seeding](#data-seeding)
6. [OSRM Setup](#osrm-setup)
7. [Start Services](#start-services)
8. [Verify Installation](#verify-installation)
9. [Common Issues](#common-issues)

---

## System Requirements

### Minimum Requirements

- **CPU:** 2 cores (4+ recommended)
- **RAM:** 4 GB (8+ GB recommended)
- **Disk:** 10 GB free space
- **OS:** Linux, macOS, or Windows (with WSL2)

### Software Requirements

| Software | Version | Purpose |
|----------|---------|---------|
| Node.js | 20+ | Runtime environment |
| npm | 10+ | Package manager |
| PostgreSQL | 15+ | Database |
| Docker | 20+ | OSRM containers |
| Docker Compose | 2.0+ | Multi-container orchestration |
| osmium-tool | 1.14+ | OSM data processing |
| Git | 2.30+ | Version control |

---

## Install Prerequisites

### 1. Install Node.js

#### Ubuntu/Debian

```bash
# Install Node.js 20.x
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# Verify installation
node --version  # Should show v20.x.x
npm --version   # Should show 10.x.x
```

#### macOS

```bash
# Using Homebrew
brew install node@20

# Verify installation
node --version
npm --version
```

#### Windows (WSL2)

```bash
# Inside WSL2 Ubuntu
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
```

### 2. Install PostgreSQL

#### Ubuntu/Debian

```bash
# Install PostgreSQL 15
sudo apt-get update
sudo apt-get install -y postgresql-15 postgresql-contrib-15

# Start PostgreSQL service
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Verify installation
psql --version  # Should show 15.x
```

#### macOS

```bash
# Using Homebrew
brew install postgresql@15
brew services start postgresql@15

# Verify installation
psql --version
```

#### Windows (WSL2)

```bash
# Install in WSL2
sudo apt-get update
sudo apt-get install -y postgresql postgresql-contrib
sudo service postgresql start
```

### 3. Install Docker & Docker Compose

#### Ubuntu/Debian

```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add user to docker group
sudo usermod -aG docker $USER
newgrp docker

# Install Docker Compose
sudo apt-get install docker-compose-plugin

# Verify installation
docker --version
docker compose version
```

#### macOS

```bash
# Install Docker Desktop from https://www.docker.com/products/docker-desktop/
# Or using Homebrew
brew install --cask docker

# Start Docker Desktop, then verify
docker --version
docker compose version
```

#### Windows

- Install Docker Desktop from https://www.docker.com/products/docker-desktop/
- Enable WSL2 backend in Docker settings
- Verify in WSL2 terminal: `docker --version`

### 4. Install osmium-tool

#### Ubuntu 22.04 (Manual Installation)

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

**Note:** Version 1.7.1 is compatible with the legacy command format. For newer versions (1.18+), the command syntax changes.

#### Ubuntu/Debian (Standard Installation)

```bash
sudo apt-get update
sudo apt-get install -y osmium-tool

# Verify installation
osmium --version  # Should show 1.14+ (or 1.18+ for modern format)
```

#### macOS

```bash
brew install osmium-tool

# Verify installation
osmium --version
```

#### Windows (WSL2)

```bash
# Inside WSL2 - use Ubuntu 22 manual installation method above
# Or try standard apt-get first:
sudo apt-get install -y osmium-tool
```

### 5. Install Git (if not already installed)

```bash
# Ubuntu/Debian
sudo apt-get install -y git

# macOS
brew install git

# Verify
git --version
```

---

## Database Setup

### 1. Create PostgreSQL User and Database

```bash
# Switch to postgres user
sudo -u postgres psql

# Inside psql shell, run these commands:
```

```sql
-- Create user
CREATE USER zone_user WITH PASSWORD 'your_secure_password';

-- Create database
CREATE DATABASE zone_db OWNER zone_user;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE zone_db TO zone_user;

-- Enable PostGIS extension (for geospatial queries)
\c zone_db
CREATE EXTENSION IF NOT EXISTS postgis;

-- Exit psql
\q
```

### 2. Test Database Connection

```bash
# Test connection
psql -h localhost -U zone_user -d zone_db -c "SELECT version();"

# Should show PostgreSQL version
```

### 3. Configure PostgreSQL (Optional but Recommended)

Edit PostgreSQL configuration for better performance:

```bash
# Find and edit postgresql.conf
sudo nano /etc/postgresql/15/main/postgresql.conf

# Recommended changes for development:
# shared_buffers = 256MB
# effective_cache_size = 1GB
# work_mem = 16MB
# maintenance_work_mem = 128MB

# Restart PostgreSQL
sudo systemctl restart postgresql
```

---

## Zone Service Setup

### 1. Clone Repository

```bash
# Navigate to your projects directory
cd /path/to/your/projects

# Clone the repository
git clone https://github.com/your-org/DS.git
cd DS/BE/zone_service
```

### 2. Install Dependencies

```bash
# Install Node.js packages
npm install

# This will install all dependencies from package.json
# Expected time: 2-3 minutes
```

### 3. Configure Environment Variables

```bash
# Create .env file
cp .env.example .env

# Edit .env file
nano .env
```

**Update `.env` with your settings:**

```bash
# Database Connection
DATABASE_URL="postgresql://zone_user:your_secure_password@localhost:5432/zone_db"

# Server Configuration
PORT=21503
NODE_ENV=development
HOST=0.0.0.0

# OSRM Configuration
OSRM_INSTANCE_1_URL=http://localhost:5000
OSRM_INSTANCE_2_URL=http://localhost:5001
OSRM_ACTIVE_INSTANCE=1

# Traffic Integration
TRACKASIA_API_KEY=your_api_key_here
TRAFFIC_UPDATE_INTERVAL=1800000  # 30 minutes in ms

# Logging
LOG_LEVEL=info
LOG_FILE=logs/zone-service.log

# Kafka (if using)
KAFKA_BROKERS=localhost:9092
KAFKA_CLIENT_ID=zone-service
KAFKA_GROUP_ID=zone-service-group
```

### 4. Generate Prisma Client

```bash
# Generate Prisma client from schema
npm run prisma:generate

# This creates TypeScript types from database schema
# Expected time: 10-15 seconds
```

### 5. Run Database Migrations

```bash
# Run all pending migrations
npm run prisma:migrate

# You'll be prompted to name the migration (just press Enter for default)
# This creates all database tables
# Expected time: 5-10 seconds
```

### 6. Verify Database Schema

```bash
# Open Prisma Studio (visual database browser)
npm run prisma:studio

# This opens http://localhost:5555
# You should see empty tables: zones, roads, road_nodes, road_segments, etc.
```

---

## Data Seeding

Now we'll populate the database with real road network data from OpenStreetMap.

### 1. Prepare OSM Data

You need OpenStreetMap data for Ho Chi Minh City. There are two options:

#### Option A: Download from Repository (Recommended)

```bash
# OSM data should be included in the repo
cd raw_data/new_hochiminh_city
ls -lh

# You should see:
# - hochiminh_city.osm.pbf  (road network data)
# - *.poly files             (district boundary files)
```

#### Option B: Download Fresh Data

```bash
# Download HCMC OSM data from Geofabrik
cd raw_data/new_hochiminh_city
wget https://download.geofabrik.de/asia/vietnam-latest.osm.pbf

# Extract only HCMC area using osmium
osmium extract vietnam-latest.osm.pbf \
  --polygon ../old_thuduc_city/thuduc_cu.poly \
  --output hochiminh_city.osm.pbf

# Clean up
rm vietnam-latest.osm.pbf
```

### 2. Seed Zones (Step 1)

```bash
# Navigate to zone_service root
cd /path/to/DS/BE/zone_service

# Seed Thu Duc districts
npm run seed:zones

# Expected output:
# ✓ Parsing 8 district polygon files
# ✓ Created 8 zones
# ✓ Generated 234 geohash cells
# ✓ Seeding completed in 28.3 seconds
```

**What this does:**
- Reads `.poly` files from `raw_data/new_hochiminh_city/`
- Creates 8 zone records (Thu Duc, Linh Xuan, Long Binh, etc.)
- Generates geohash cells for spatial indexing
- Inserts data into `zones` and `zone_geohash_cells` tables

### 3. Seed Roads (Step 2)

```bash
# Seed road network (THIS IS FAST - 70x improved!)
npm run seed:roads

# Expected output:
# ✓ Extracting Thu Duc roads from OSM
# ✓ Found 4,957 roads
# ✓ Creating road nodes...
# ✓ Created 9,914 nodes
# ✓ Creating road segments...
# ✓ Created 4,957 segments
# ✓ Merging duplicate nodes...
# ✓ Merged 3,227 duplicates (9,914 → 6,588 nodes)
# ✓ Calculating base weights...
# ✓ Seeding completed in 51.2 seconds
```

**What this does:**
- Extracts roads from `hochiminh_city.osm.pbf` within Thu Duc boundary
- Creates records in `roads` table
- Creates nodes in `road_nodes` table (intersections + waypoints)
- Creates segments in `road_segments` table
- **Automatically merges duplicate nodes** using coordinate matching (1.1m precision)
- Calculates `base_weight` for each segment based on length, speed, road type

**Performance:**
- Old seeder: 1+ hour for 17k streets
- New seeder: **51 seconds** for 17k streets (70x faster!)

### 4. Verify Seeded Data

```bash
# Open Prisma Studio
npm run prisma:studio

# Check the data:
# - zones: Should have 8 records
# - roads: Should have ~4,957 records
# - road_nodes: Should have ~6,588 records
# - road_segments: Should have ~4,957 records

# Or use SQL directly
psql -h localhost -U zone_user -d zone_db -c "
  SELECT
    (SELECT COUNT(*) FROM zones) as zones,
    (SELECT COUNT(*) FROM roads) as roads,
    (SELECT COUNT(*) FROM road_nodes) as nodes,
    (SELECT COUNT(*) FROM road_segments) as segments;
"
```

### 5. Check Road Network Connectivity (Optional)

```bash
# Run connectivity analysis
npm run check:connectivity

# Expected output:
# ✓ Building road network graph...
# ✓ Found 6,588 nodes and 4,957 segments
# ✓ Analyzing connectivity...
# ✓ Found 1,949 connected components
# ✓ Largest component: 1,937 nodes (29.40%)
# ✓ Analysis completed in 2.1 seconds
```

**What this means:**
- **1,949 components** = There are isolated road groups (expected due to Thu Duc boundary crop)
- **Largest component 29.40%** = 1,937 nodes are connected in main network
- **This is normal and expected** for a cropped geographic area

---

## OSRM Setup

Now we'll set up the OSRM routing engine using the seeded data.

### 1. Generate OSRM Data

```bash
# Generate OSRM routing files from database
npm run osrm:generate

# Expected output:
# ✓ Exporting road network from database...
# ✓ Found 6,588 nodes and 4,957 segments
# ✓ Creating OSM XML with custom weights...
# ✓ Generated network.osm.xml (3.2 MB)
# ✓ Creating custom Lua profile...
# ✓ Generated custom_car.lua
# ✓ Running osrm-extract...
# ✓ Extract completed in 8.3 seconds
# ✓ Running osrm-partition...
# ✓ Partition completed in 12.7 seconds
# ✓ Running osrm-customize...
# ✓ Customize completed in 5.1 seconds
# ✓ OSRM data generated successfully!
# ✓ Total time: 1 minute 58 seconds
```

**What this does:**
- Exports road network from PostgreSQL to OSM XML format
- Creates custom Lua profile with dynamic weights
- Runs OSRM preprocessing steps:
  1. **osrm-extract** - Parse OSM XML and build road graph
  2. **osrm-partition** - Divide graph into cells for MLD algorithm
  3. **osrm-customize** - Apply custom weights to edges
- Generates files in `osrm_data/osrm-instance-1/`:
  - `network.osrm` (routing graph)
  - `network.osrm.edges` (edge data)
  - `network.osrm.cells` (partition data)
  - `network.osrm.mldgr` (multi-level graph)

### 2. Verify OSRM Files

```bash
# Check generated files
ls -lh osrm_data/osrm-instance-1/

# You should see:
# -rw-r--r-- network.osm.xml       (~3 MB)
# -rw-r--r-- custom_car.lua        (~2 KB)
# -rw-r--r-- network.osrm          (~5 MB)
# -rw-r--r-- network.osrm.edges    (~1 MB)
# -rw-r--r-- network.osrm.cells    (~500 KB)
# -rw-r--r-- network.osrm.mldgr    (~3 MB)
# -rw-r--r-- network.osrm.partition (~100 KB)
```

### 3. Copy Data to Instance 2 (for dual-instance setup)

```bash
# Copy all OSRM files to instance 2
cp -r osrm_data/osrm-instance-1/* osrm_data/osrm-instance-2/

# Verify
ls -lh osrm_data/osrm-instance-2/
```

---

## Start Services

### 1. Start OSRM Instances with Docker

```bash
# Navigate to project root (where docker-compose.yml is)
cd /path/to/DS

# Start OSRM containers
docker-compose up -d osrm-instance-1 osrm-instance-2

# Check container status
docker ps | grep osrm

# You should see:
# dss-osrm-1  (healthy)  0.0.0.0:5000->5000/tcp
# dss-osrm-2  (healthy)  0.0.0.0:5001->5000/tcp

# Check logs
docker logs dss-osrm-1
docker logs dss-osrm-2

# Both should show:
# [info] starting up engines, v5.x.x
# [info] http 0.0.0.0:5000 compression handled by proxy
# [info] running and waiting for requests
```

**Troubleshooting:**
If containers show as "unhealthy", check:

```bash
# Check detailed container status
docker inspect dss-osrm-1 | grep -A 10 "Health"

# Try manual healthcheck
docker exec dss-osrm-1 timeout 2 bash -c '</dev/tcp/localhost/5000'

# If that fails, check OSRM data files
docker exec dss-osrm-1 ls -la /data/
```

### 2. Test OSRM Instances Directly

```bash
# Test Instance 1
curl "http://localhost:5000/route/v1/driving/106.7718,10.8505;106.8032,10.8623?overview=false"

# Expected response:
# {"code":"Ok","routes":[{"distance":6400.5,"duration":510.2}],"waypoints":[...]}

# Test Instance 2
curl "http://localhost:5001/route/v1/driving/106.7718,10.8505;106.8032,10.8623?overview=false"

# Should return similar result
```

### 3. Start Zone Service

```bash
# Navigate back to zone_service
cd /path/to/DS/BE/zone_service

# Start in development mode (with hot reload)
npm run dev

# Expected output:
# [INFO] Zone Service starting...
# [INFO] Database connected
# [INFO] Prisma Client initialized
# [INFO] OSRM Router initialized (active instance: 1)
# [INFO] Server listening on http://0.0.0.0:21503
# [INFO] Zone Service ready!
```

**Keep this terminal open** - this is your zone service running.

---

## Verify Installation

Open a **new terminal** and run these verification tests:

### 1. Health Check

```bash
curl http://localhost:21503/api/v1/health

# Expected response:
# {
#   "status": "healthy",
#   "service": "zone-service",
#   "version": "1.0.0",
#   "timestamp": "2025-01-15T10:30:00.000Z"
# }
```

### 2. Test Routing API

```bash
# Test single route
curl -X POST http://localhost:21503/api/v1/routing/route \
  -H "Content-Type: application/json" \
  -d '{
    "waypoints": [
      {"lat": 10.8505, "lon": 106.7718},
      {"lat": 10.8623, "lon": 106.8032}
    ]
  }'

# Expected response:
# {
#   "code": "Ok",
#   "routes": [{
#     "distance": 6400.5,
#     "duration": 510.2,
#     "weight": 548.7,
#     "legs": [...]
#   }]
# }
```

### 3. Test Zones API

```bash
# Get all zones
curl http://localhost:21503/api/v1/zones

# Expected response:
# {
#   "data": [
#     {"id": 1, "name": "Thủ Đức", "code": "TD", ...},
#     {"id": 2, "name": "Linh Xuân", "code": "LX", ...},
#     ...
#   ],
#   "paging": {
#     "page": 0,
#     "limit": 10,
#     "total": 8
#   }
# }
```

### 4. Run Stress Test

```bash
# Run hard routing stress test
cd /path/to/DS/BE/zone_service
npx tsx test-osrm-hard-routes.ts

# Expected output:
# 🔥 OSRM Hard Stress Test
# Testing routes: Phạm Văn Đồng → Man Thiện Street
#
# Test 1/3: ✅ SUCCESS (6.40 km, 8.5 min)
# Test 2/3: ✅ SUCCESS (12.12 km, 12.8 min)
# Test 3/3: ✅ SUCCESS (7.21 km, 9.7 min)
#
# 📊 TEST RESULTS SUMMARY
# Success Rate: 100.0%
# 🏆 Grade: EXCELLENT
```

### 5. Explore Data in Prisma Studio

```bash
# Open Prisma Studio
npm run prisma:studio

# Opens http://localhost:5555
# Browse your data visually
```

---

## Common Issues

### Issue 1: "Cannot connect to database"

**Symptoms:**
```
Error: connect ECONNREFUSED 127.0.0.1:5432
```

**Solutions:**

```bash
# 1. Check PostgreSQL is running
sudo systemctl status postgresql  # Linux
brew services list | grep postgresql  # macOS

# 2. Start PostgreSQL if stopped
sudo systemctl start postgresql  # Linux
brew services start postgresql@15  # macOS

# 3. Test connection manually
psql -h localhost -U zone_user -d zone_db

# 4. Check DATABASE_URL in .env
# Make sure host, user, password, and database name are correct
```

### Issue 2: "OSRM container unhealthy"

**Symptoms:**
```
docker ps shows (unhealthy) status
```

**Solutions:**

```bash
# 1. Check logs
docker logs dss-osrm-1

# 2. Common issue: Missing .osrm files
# Solution: Regenerate OSRM data
npm run osrm:generate
docker-compose restart osrm-instance-1 osrm-instance-2

# 3. Check healthcheck manually
docker exec dss-osrm-1 timeout 2 bash -c '</dev/tcp/localhost/5000'

# 4. Verify data files exist
docker exec dss-osrm-1 ls -la /data/
# Should show network.osrm and related files
```

### Issue 3: "OSRM returns NoRoute"

**Symptoms:**
```
{"code":"NoRoute","message":"No route found"}
```

**Solutions:**

```bash
# 1. Check road network connectivity
npm run check:connectivity

# 2. Verify coordinates are within Thu Duc area
# Thu Duc bounds: lat 10.8-10.9, lon 106.7-106.8

# 3. Try coordinates from database
npm run prisma:studio
# Copy actual node coordinates and test with those

# 4. Regenerate OSRM data
npm run osrm:generate
docker-compose restart osrm-instance-1 osrm-instance-2
```

### Issue 4: "Seeding too slow"

**Symptoms:**
```
npm run seed:roads taking more than 5 minutes
```

**Solutions:**

```bash
# 1. Make sure you have latest code
git pull origin main
npm install

# 2. New seeder should be ~51 seconds for 17k roads
# If still slow, check:

# 3. PostgreSQL performance settings
sudo nano /etc/postgresql/15/main/postgresql.conf
# Increase: shared_buffers, work_mem

# 4. Check disk space
df -h

# 5. Check CPU usage
htop
```

### Issue 5: "osmium-tool not found" or "Incompatible version"

**Symptoms:**
```
bash: osmium: command not found
# Or
Command failed: osmium extract ...
```

**Solutions:**

**For Ubuntu 22.04 (if apt-get doesn't work or installs incompatible version):**

```bash
# Manual installation with specific version (1.7.1)
# Download and install boost dependency
wget http://archive.ubuntu.com/ubuntu/pool/main/b/boost1.65.1/libboost-program-options1.65.1_1.65.1+dfsg-0ubuntu5_amd64.deb
sudo dpkg -i libboost-program-options1.65.1_1.65.1+dfsg-0ubuntu5_amd64.deb

# Download and install osmium-tool 1.7.1
wget http://launchpadlibrarian.net/344172654/osmium-tool_1.7.1-1_amd64.deb
sudo dpkg -i osmium-tool_1.7.1-1_amd64.deb

# Verify installation
osmium --version  # Should show 1.7.1
```

**For other Ubuntu/Debian versions:**

```bash
sudo apt-get update
sudo apt-get install -y osmium-tool

# Verify installation
osmium --version
```

**For macOS:**

```bash
brew install osmium-tool

# Verify installation
osmium --version
```

**Note:** The code automatically detects osmium version and uses the appropriate command format:
- Version >= 1.18: Modern format with `-s complete_ways --overwrite input -o output --polygon poly`
- Version < 1.18: Legacy format with `-p poly -s complete_ways -O -o output input`

### Issue 6: "Port 21503 already in use"

**Symptoms:**
```
Error: listen EADDRINUSE: address already in use :::21503
```

**Solutions:**

```bash
# 1. Find process using port
sudo lsof -i :21503  # Linux/macOS
netstat -ano | findstr :21503  # Windows

# 2. Kill the process
kill -9 <PID>

# 3. Or change port in .env
# Edit .env and change PORT=21503 to PORT=21504
```

---

## Next Steps

After successful installation:

1. **Read Architecture Docs**
   - [ARCHITECTURE.md](./ARCHITECTURE.md) - Understand system design
   - [OSRM.md](./OSRM.md) - Deep dive into OSRM integration

2. **Explore API**
   - [API.md](./API.md) - Complete API reference
   - Test endpoints with Postman or curl

3. **Set Up Development Workflow**
   - [WORKFLOWS.md](./WORKFLOWS.md) - Daily development practices

4. **Integrate with Your App**
   - Connect mobile app to API Gateway
   - API Gateway proxies to Zone Service

---

## Useful Commands Reference

```bash
# Service Management
npm run dev              # Start development server
npm run build            # Build for production
npm run start            # Start production server

# Database
npm run prisma:studio    # Visual database browser
npm run prisma:migrate   # Run migrations
npm run prisma:generate  # Generate Prisma client

# Data
npm run seed:zones       # Seed zones
npm run seed:roads       # Seed roads (fast!)
npm run check:connectivity  # Analyze network

# OSRM
npm run osrm:generate    # Generate OSRM data
docker-compose restart osrm-instance-1  # Restart OSRM

# Testing
npx tsx test-osrm-hard-routes.ts  # Stress test
npm run check:connectivity         # Network analysis

# Docker
docker ps                         # List containers
docker logs dss-osrm-1           # View logs
docker-compose up -d             # Start all services
docker-compose down              # Stop all services
docker exec -it dss-osrm-1 bash  # Enter container
```

---

## Troubleshooting Resources

- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) - Complete troubleshooting guide
- [OSRM Documentation](http://project-osrm.org/docs/)
- [Prisma Documentation](https://www.prisma.io/docs)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

**Setup complete!** 🎉

You now have a fully functional Zone Service with:
- ✅ PostgreSQL database with 4,957 roads
- ✅ Dual OSRM instances for routing
- ✅ Zone Service API running
- ✅ 100% routing success rate

**Next:** Read [WORKFLOWS.md](./WORKFLOWS.md) to learn daily development practices.
