# OSRM Management System - Development Testing Guide

## Prerequisites

1. **Node.js >= 20.0.0**
2. **Database**: MySQL database với zone_service schema
3. **Docker**: Để chạy OSRM processing (optional cho extract test)
4. **osmium-tool**: Để test extract functionality (optional)

## Setup Development Environment

```bash
cd osrm-management-system

# 1. Copy env file
cp env.local .env

# 2. Install dependencies
npm install

# 3. Generate Prisma client
npm run prisma:generate

# 4. Start dev server
npm run dev
```

### Edit Configuration

```bash
# Copy env file
cp env.local .env

# Edit .env với config của bạn
# - DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD
# - ZONE_DB_NAME: database name của zone_service
# - RAW_DATA_PATH: đường dẫn đến raw_data (có thể dùng relative path)
# - OSRM_DATA_PATH: đường dẫn đến osrm_data (có thể dùng relative path)
```

### Verify Setup

Khi chạy `npm run dev`, nếu database connection thành công, bạn sẽ thấy log:
```
Database connected successfully
OSRM Management System started on port 21520
```

## Testing Methods

### Method 1: Development Server (Recommended)

Chạy server với hot-reload:

```bash
npm run dev
```

Server sẽ chạy trên `http://localhost:21520`

**Advantages:**
- Hot-reload khi code thay đổi
- See logs in real-time
- Easy debugging

### Method 2: Build and Run

```bash
# Build
npm run build

# Run
npm start
```

## Testing API Endpoints

### 1. Health Check

```bash
# Test health endpoint
curl http://localhost:21520/api/v1/health

# Expected response:
# {
#   "result": {
#     "status": "healthy",
#     "timestamp": "2024-...",
#     "service": "osrm-management-system"
#   }
# }
```

### 2. Extract Complete Data

**Prerequisites:**
- osmium-tool installed
- Raw data files available tại `RAW_DATA_PATH`
- Polygon file available (default: `raw_data/poly/hcmc.poly`)

```bash
# Extract với default polygon
curl -X POST http://localhost:21520/api/v1/extract/complete \
  -H "Content-Type: application/json"

# Extract với custom polygon file
curl -X POST http://localhost:21520/api/v1/extract/complete \
  -H "Content-Type: application/json" \
  -d '{"polyFile": "./raw_data/poly/hcmc.poly"}'
```

**Expected Response:**
```json
{
  "result": {
    "success": true,
    "outputPath": "./raw_data/extracted/thuduc_complete.osm.pbf",
    "duration": 12345
  },
  "message": "Extraction completed successfully"
}
```

**Testing Notes:**
- Extract có thể mất vài phút tùy vào kích thước data
- Kiểm tra logs để theo dõi progress
- Output file sẽ được tạo tại `RAW_DATA_PATH/extracted/`

### 3. Generate OSRM V2 Models

**Prerequisites:**
- Database có đầy đủ data (roads, road_nodes, road_segments, user_feedback, traffic_conditions)
- Docker installed và running
- OSRM Docker image available

```bash
# Generate all OSRM V2 models
curl -X POST http://localhost:21520/api/v1/generate/osrm-v2 \
  -H "Content-Type: application/json"
```

**Expected Response:**
```json
{
  "result": {
    "success": true,
    "models": [
      "osrm-full",
      "osrm-rating-only",
      "osrm-blocking-only",
      "osrm-base"
    ],
    "duration": 1234567
  },
  "message": "Generated 4 OSRM models successfully"
}
```

**Testing Notes:**
- Generation có thể mất rất lâu (30 phút - vài giờ) tùy vào kích thước data
- Kiểm tra logs để theo dõi progress
- Output files sẽ được tạo tại `OSRM_DATA_PATH/{model-name}/`
- Mỗi model sẽ có: `network.osm.xml`, `custom_bicycle.lua`, và các `.osrm` files

## Testing with Postman/Thunder Client

### Import Collection

Tạo collection với các requests:

1. **Health Check**
   - Method: GET
   - URL: `http://localhost:21520/api/v1/health`

2. **Extract Complete**
   - Method: POST
   - URL: `http://localhost:21520/api/v1/extract/complete`
   - Headers: `Content-Type: application/json`
   - Body (optional):
     ```json
     {
       "polyFile": "./raw_data/poly/hcmc.poly"
     }
     ```

3. **Generate OSRM V2**
   - Method: POST
   - URL: `http://localhost:21520/api/v1/generate/osrm-v2`
   - Headers: `Content-Type: application/json`

## Testing Individual Services

### Test Extract Service

Tạo test script `test-extract.ts`:

```typescript
import { ExtractService } from './src/services/extract.service';

async function testExtract() {
  const service = new ExtractService();
  const result = await service.extractCompleteData();
  console.log('Extract result:', result);
}

testExtract();
```

Run:
```bash
tsx test-extract.ts
```

### Test Generate Service

Tạo test script `test-generate.ts`:

```typescript
import { PrismaClient } from '@prisma/client';
import { GenerateService } from './src/services/generate.service';

async function testGenerate() {
  const prisma = new PrismaClient({
    datasources: {
      db: {
        url: process.env.DB_CONNECTION_STRING || 'mysql://root:root@127.0.0.1:3306/ds_zone_service',
      },
    },
  });

  await prisma.$connect();
  
  const service = new GenerateService(prisma);
  const result = await service.generateAllModels();
  console.log('Generate result:', result);
  
  await prisma.$disconnect();
}

testGenerate();
```

Run:
```bash
tsx test-generate.ts
```

## Common Issues and Solutions

### 1. Database Connection Failed

**Error:**
```
Error: P1001: Can't reach database server
```

**Solutions:**
- Kiểm tra database đang chạy
- Kiểm tra DB_HOST, DB_PORT trong .env.local
- Kiểm tra firewall/network
- Test connection: `mysql -h DB_HOST -P DB_PORT -u DB_USERNAME -p`

### 2. Prisma Client Not Generated

**Error:**
```
Error: Cannot find module '@prisma/client'
```

**Solution:**
```bash
npm run prisma:generate
```

### 3. osmium-tool Not Found

**Error:**
```
osmium-tool is not installed
```

**Solution:**
```bash
# Ubuntu/Debian
sudo apt-get install osmium-tool

# macOS
brew install osmium-tool

# Windows (via WSL or Docker)
```

### 4. Docker Not Running

**Error:**
```
Cannot connect to the Docker daemon
```

**Solution:**
- Start Docker Desktop
- Kiểm tra: `docker ps`
- Nếu không có Docker, có thể skip generate test (chỉ test extract)

### 5. Path Not Found

**Error:**
```
Poly file not found: ./raw_data/poly/hcmc.poly
```

**Solution:**
- Kiểm tra RAW_DATA_PATH trong .env.local
- Đảm bảo paths là relative đến working directory hoặc absolute
- Tạo thư mục nếu chưa có: `mkdir -p raw_data/poly`

### 6. Port Already in Use

**Error:**
```
Error: listen EADDRINUSE: address already in use :::21520
```

**Solution:**
- Change PORT trong .env.local
- Hoặc kill process đang dùng port:
  ```bash
  # Find process
  lsof -i :21520
  # Kill process
  kill -9 <PID>
  ```

## Debugging Tips

### 1. Enable Verbose Logging

Trong `.env.local`:
```env
NODE_ENV=development
```

Logger sẽ output ở level `debug` thay vì `info`.

### 2. Check Logs

Logs được ghi vào:
- Console (stdout)
- `logs/combined.log`
- `logs/error.log`

### 3. Test Database Queries

```bash
# Connect to database
mysql -h DB_HOST -P DB_PORT -u DB_USERNAME -p ZONE_DB_NAME

# Check tables
SHOW TABLES;

# Check data
SELECT COUNT(*) FROM roads;
SELECT COUNT(*) FROM road_segments;
```

### 4. Monitor Docker Containers

```bash
# List running containers
docker ps

# Check Docker logs (if using Docker for OSRM)
docker logs <container-id>
```

## Quick Test Checklist

- [ ] Dependencies installed (`npm install`)
- [ ] Prisma client generated (`npm run prisma:generate`)
- [ ] .env.local configured
- [ ] Database accessible
- [ ] Server starts successfully (`npm run dev`)
- [ ] Health check works (`GET /api/v1/health`)
- [ ] Extract works (nếu có osmium-tool và data)
- [ ] Generate works (nếu có database data và Docker)

## Next Steps

Sau khi test thành công:
1. Test với production-like data
2. Test error handling
3. Test với large datasets
4. Performance testing
5. Integration testing với zone_service
