# Zone Service

Microservice for zone and location management in the Delivery System. Handles geographical zones, centers, and spatial data management.

## 🏗️ Architecture

### Technology Stack

- **Runtime**: Node.js 20+
- **Language**: TypeScript
- **Framework**: Express.js
- **Database**: MySQL with Prisma ORM
- **Message Queue**: Kafka (optional)
- **Logging**: Winston
- **Validation**: class-validator & class-transformer

### Project Structure

```
zone_service/
├── src/
│   ├── common/                    # Reusable common modules
│   │   ├── database/              # Prisma singleton client
│   │   ├── logger/                # Winston logger service
│   │   ├── kafka/                 # Kafka service (background job)
│   │   ├── health/                # Health check endpoints
│   │   ├── startup/               # Startup flow (settings check & init)
│   │   ├── decorators/            # Custom decorators for validation
│   │   ├── middleware/            # Express middleware (error, logging)
│   │   ├── modules/               # Common services (settings)
│   │   └── types/                 # TypeScript types & DTOs
│   │       └── restful/           # RESTful response formats
│   ├── modules/                   # Business modules
│   │   ├── center/                # Center management
│   │   │   ├── center.interface.ts
│   │   │   ├── center.model.ts    # DTOs & request models
│   │   │   ├── center.service.ts  # Static class service
│   │   │   ├── center.controller.ts
│   │   │   └── index.ts
│   │   ├── zone/                  # Zone management
│   │   │   ├── zone.interface.ts
│   │   │   ├── zone.model.ts
│   │   │   ├── zone.service.ts
│   │   │   ├── zone.controller.ts
│   │   │   └── index.ts
│   │   ├── index.ts
│   │   └── routes.ts              # Main routes configuration
│   ├── app.ts                     # Express app configuration
│   └── index.ts                   # Main entry point
├── prisma/
│   ├── schema.prisma              # Prisma schema
│   └── models/                    # Separated model files
│       ├── centers.prisma
│       ├── zones.prisma
│       └── ...
├── logs/                          # Application logs
├── env.local                      # Local environment variables
├── tsconfig.json                  # TypeScript configuration
├── package.json
└── README.md
```

## 🚀 Getting Started

### Prerequisites

- Node.js >= 20.0.0
- npm >= 10.0.0
- MySQL 8.0+
- Settings Service (running on port 3001)

### Installation

```bash
# Install dependencies
npm install

# Generate Prisma client
npm run prisma:generate

# Run database migrations
npm run prisma:migrate
```

### Environment Configuration

Copy `env.local` and configure:

```env
# Database Configuration
ZONE_DB_CONNECTION=mysql://root:root@localhost:3306/ds_zone_service

# Settings Service Configuration
SETTINGS_SERVICE_URL=http://localhost:3001
SETTINGS_SERVICE_TIMEOUT=10000

# Server Configuration
NODE_ENV=development
PORT=21503
LOG_LEVEL=info

# CORS Configuration (comma-separated origins)
CORS_ORIGINS=http://localhost:3000,http://localhost:5173,http://localhost:8080

# Kafka Configuration (optional)
# KAFKA_BROKERS=localhost:9092
# KAFKA_GROUP_ID=zone-service-group
```

### Running the Service

```bash
# Development mode with hot reload
npm run dev

# Build for production
npm run build

# Run production build
npm start

# Run with production environment
npm run start:prod
```

## 📋 API Documentation

### Base URL

```
http://localhost:21503/api/v1
```

### Health Check Endpoints

- `GET /health` - Basic health check
- `GET /health/detailed` - Detailed health with dependencies
- `GET /health/readiness` - Kubernetes readiness probe
- `GET /health/liveness` - Kubernetes liveness probe

### Center Endpoints

#### Get All Centers
```http
GET /api/v1/centers?page=0&size=10&search=keyword
```

Response:
```json
{
  "result": {
    "data": [
      {
        "id": "uuid",
        "code": "CTR001",
        "name": "Center Name",
        "address": "Address",
        "lat": 10.762622,
        "lon": 106.660172,
        "polygon": {}
      }
    ],
    "page": {
      "page": 0,
      "size": 10,
      "totalElements": 100,
      "totalPages": 10
    }
  }
}
```

#### Get Center by ID
```http
GET /api/v1/centers/:id
```

#### Get Center by Code
```http
GET /api/v1/centers/code/:code
```

#### Create Center
```http
POST /api/v1/centers
Content-Type: application/json

{
  "code": "CTR001",
  "name": "Center Name",
  "address": "Address",
  "lat": 10.762622,
  "lon": 106.660172,
  "polygon": {}
}
```

#### Update Center
```http
PUT /api/v1/centers/:id
Content-Type: application/json

{
  "name": "Updated Name",
  "address": "New Address"
}
```

#### Delete Center
```http
DELETE /api/v1/centers/:id
```

### Zone Endpoints

Similar structure to Center endpoints:

- `GET /api/v1/zones` - Get all zones with pagination
- `GET /api/v1/zones/:id` - Get zone by ID
- `GET /api/v1/zones/code/:code` - Get zone by code
- `GET /api/v1/zones/center/:centerId` - Get zones by center
- `POST /api/v1/zones` - Create zone
- `PUT /api/v1/zones/:id` - Update zone
- `DELETE /api/v1/zones/:id` - Delete zone

## 🔧 Development Guide

### Module Structure

Each module follows this structure:

1. **Interface** (`*.interface.ts`) - Service contract
2. **Model** (`*.model.ts`) - DTOs and request models
3. **Service** (`*.service.ts`) - Static class with business logic
4. **Controller** (`*.controller.ts`) - HTTP request handlers & router
5. **Index** (`index.ts`) - Named exports (no default exports)

### Creating a New Module

```typescript
// 1. Create interface
export interface IMyService {
  doSomething(): Promise<void>;
}

// 2. Create models/DTOs
export class MyDto {
  @IsString()
  @IsNotEmpty()
  name!: string;
}

export class MyPagingRequest extends PagingRequest {
  // Add specific filters
}

// 3. Create service (static class)
export class MyService implements IMyService {
  public static async doSomething(): Promise<void> {
    // Implementation
  }
}

// 4. Create controller & router
export class MyController {
  public static async handleRequest(req: Request, res: Response): Promise<void> {
    const result = await MyService.doSomething();
    res.json(BaseResponse.success(result));
  }
}

export const myRouter = Router();
myRouter.get('/', MyController.handleRequest);

// 5. Export in index.ts
export { MyService as myService } from './my.service';
export { myRouter } from './my.controller';
```

### Using Decorators

```typescript
import { ValidateBody, ValidateQuery } from '../../common/decorators';

class MyController {
  @ValidateBody(CreateDto)
  public static async create(req: Request, res: Response): Promise<void> {
    // req.body is already validated
  }
}
```

### Response Format

All responses follow the standard format:

```typescript
// Success
res.json(BaseResponse.success(data, 'Optional message'));

// Error
res.status(400).json(BaseResponse.error('Error message', [errors]));

// With pagination
const pagedData = new PagedData(items, paging);
res.json(BaseResponse.success(pagedData));
```

## 🔄 Startup Flow

The service follows this startup sequence:

1. **Settings Service Check** (10 retries, 10s delay)
   - Validates Settings Service availability
   - Required if `SETTINGS_SERVICE_URL` is configured

2. **Settings Initialization**
   - Creates default setting keys if not exist
   - Zone configuration, feature flags, etc.

3. **Database Connection**
   - Tests Prisma connection with `SELECT 1`

4. **Kafka Initialization** (optional, non-blocking)
   - Runs as background job
   - Service continues if Kafka fails

5. **Express Server Start**
   - Configures CORS, middleware, routes
   - Starts health check endpoints

## 🧪 Testing

```bash
# Run tests (when implemented)
npm test

# Open Prisma Studio
npm run prisma:studio
```

## 🐳 Docker

```bash
# Build Docker image
docker build -t zone-service .

# Run container
docker run -p 21503:21503 --env-file env.local zone-service
```

## 📝 Design Principles

### Static Class Services
- All services are static classes
- No instantiation required
- Implements interface for contract

### Singleton Pattern
- Prisma client is singleton
- Logger is singleton
- Kafka service is singleton

### Background Jobs
- Kafka runs as background job (non-blocking)
- Uses `setImmediate` for async operations
- Doesn't block main thread

### RESTful Standards
- Request DTOs can extend `PagingRequest`
- Response DTOs use `BaseResponse<T>`
- Paged responses use `PagedData<T>`

### Validation
- Use class-validator decorators
- Automatic validation in controllers
- Clear error messages

## 🔒 Security

- CORS configured from environment
- API Gateway handles authentication
- No sensitive data in logs
- Graceful shutdown handling

## 📊 Monitoring

- Winston logger with levels
- Health check endpoints
- Request/response logging
- Error tracking

## 🤝 Contributing

1. Follow existing module structure
2. Use static classes for services
3. Implement interfaces
4. Add proper validation
5. Use standard response format
6. Update this README

## 📄 License

ISC

## 🆘 Troubleshooting

### Settings Service Not Available
- Check `SETTINGS_SERVICE_URL` in env
- Verify Settings Service is running
- Check network connectivity

### Database Connection Failed
- Verify MySQL is running
- Check `ZONE_DB_CONNECTION` URL
- Run migrations: `npm run prisma:migrate`

### Prisma Client Not Generated
```bash
npm run prisma:generate
```

### Port Already in Use
- Change `PORT` in env.local
- Kill process using port 21503

## 📞 Support

For issues or questions, contact the development team.
