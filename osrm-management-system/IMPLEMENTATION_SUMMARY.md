# OSRM Management System - Implementation Summary

## Tổng quan

Đã tạo một service độc lập hoàn toàn để quản lý việc build dữ liệu OSRM. Service này sẽ được build và copy vào `prod-pack` để chạy trên VPS.

## Cấu trúc

```
osrm-management-system/
├── src/
│   ├── common/
│   │   ├── logger.ts              # Winston logger
│   │   └── types/
│   │       └── restful.ts         # RESTful response types
│   ├── controllers/
│   │   ├── extract.controller.ts  # Extract endpoints
│   │   ├── generate.controller.ts # Generate endpoints
│   │   └── health.controller.ts   # Health check
│   ├── services/
│   │   ├── extract.service.ts     # Extract service logic
│   │   └── generate.service.ts    # Generate service logic
│   ├── routes/
│   │   └── index.ts               # Route configuration
│   ├── utils/
│   │   ├── osmium-wrapper.ts      # Osmium wrapper (independent copy)
│   │   └── osm-parser.ts          # OSM parser utilities
│   ├── app.ts                      # Express app setup
│   └── index.ts                    # Entry point
├── prisma/
│   └── schema.prisma               # Prisma schema (roads, nodes, segments, etc.)
├── env.local                       # Environment template
├── package.json
├── tsconfig.json
└── README.md
```

## Tính năng

### 1. Extract Service
- Extract complete OSM data (routing + addresses) từ PBF files
- Sử dụng osmium-tool để extract với polygon boundary
- API: `POST /api/v1/extract/complete`

### 2. Generate Service
- Generate OSRM V2 models từ database
- Tạo 4 models: osrm-full, osrm-rating-only, osrm-blocking-only, osrm-base
- Sử dụng Docker để chạy OSRM tools
- API: `POST /api/v1/generate/osrm-v2`

### 3. RESTful API
- Tuân thủ chuẩn RESTFUL.md
- BaseResponse wrapper cho tất cả responses
- Health check endpoint: `GET /api/v1/health`

## Configuration

### Environment Variables

Service sử dụng `.env` file trực tiếp (không cần config file riêng). File `env.local` chứa các config mẫu:

```env
PORT=21520
NODE_ENV=production
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=root
ZONE_DB_NAME=ds_zone_service
RAW_DATA_PATH=./raw_data
OSRM_DATA_PATH=./osrm_data
OSRM_DOCKER_IMAGE=osrm/osrm-backend:latest
```

### Paths

Tất cả paths là relative đến working directory (wwwroot/dss/):
- `RAW_DATA_PATH`: Thư mục chứa raw OSM data
- `OSRM_DATA_PATH`: Thư mục chứa OSRM data đã build

## CI/CD Integration

### Workflow Updates

Đã cập nhật `.github/workflows/prod-pack-ci-cd.yml`:

1. **Trigger**: Kích hoạt khi có thay đổi trong `osrm-management-system/`
2. **Build Step**: 
   - Setup Node.js 20
   - Install dependencies
   - Build TypeScript
   - Copy build output vào `prod-pack/osrm-management-system/`
3. **Deploy**: Copy vào prod branch cùng với prod-pack

### Build Output Structure

Sau khi build, `prod-pack/osrm-management-system/` sẽ chứa:
- `dist/` - Compiled JavaScript
- `package.json` - Dependencies
- `prisma/` - Prisma schema
- `env.local` - Environment template
- `logs/` - Log directory (created at runtime)

## Usage trên VPS

### 1. Setup

```bash
cd wwwroot/dss/osrm-management-system
cp env.local .env
# Edit .env với config thực tế
npm install --production
npx prisma generate
```

### 2. Start Service

```bash
node dist/index.js
```

Hoặc sử dụng PM2/systemd để chạy như daemon:

```bash
pm2 start dist/index.js --name osrm-management
```

### 3. API Usage

```bash
# Health check
curl http://localhost:21520/api/v1/health

# Extract complete data
curl -X POST http://localhost:21520/api/v1/extract/complete \
  -H "Content-Type: application/json" \
  -d '{"polyFile": "./raw_data/poly/hcmc.poly"}'

# Generate OSRM V2 models
curl -X POST http://localhost:21520/api/v1/generate/osrm-v2
```

## Dependencies

### Runtime Dependencies
- `express` - Web framework
- `@prisma/client` - Database client
- `winston` - Logging
- `cors` - CORS middleware
- `dotenv` - Environment variables

### Build Dependencies
- `typescript` - TypeScript compiler
- `tsc-alias` - Path alias resolver
- `prisma` - Prisma CLI
- `tsx` - TypeScript execution (dev only)

### System Requirements
- Node.js >= 20.0.0
- Docker (for OSRM processing)
- osmium-tool (for OSM extraction)
- MySQL database (zone_service database)

## Module Structure

### Độc lập hoàn toàn
- Tất cả code được sao chép độc lập từ zone_service
- Không phụ thuộc vào zone_service codebase
- Có thể chạy standalone

### Module hóa
- **Config**: Environment và configuration management
- **Services**: Business logic (extract, generate)
- **Controllers**: HTTP request handlers
- **Utils**: Utility functions (osmium wrapper, OSM parser)
- **Routes**: Route configuration

### RESTful API
- Tuân thủ chuẩn RESTFUL.md
- BaseResponse wrapper
- Error handling chuẩn
- Health check endpoint

## Next Steps

1. **Testing**: Test các endpoints trên VPS
2. **Monitoring**: Setup monitoring và alerting
3. **Documentation**: Bổ sung API documentation chi tiết
4. **Security**: Thêm authentication nếu cần
5. **Performance**: Optimize cho large datasets

## Notes

- Service chạy trên port 21520 mặc định
- Tất cả paths là relative đến working directory
- Cần database connection đến zone_service database
- Cần Docker để chạy OSRM processing
- Cần osmium-tool để extract OSM data
