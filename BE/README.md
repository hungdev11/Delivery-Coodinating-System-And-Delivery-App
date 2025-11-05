# Backend Services - Delivery System

## Overview

The backend architecture of the Delivery System follows a microservices pattern with multiple independent services communicating through RESTful APIs. All client requests are routed through the API Gateway, which handles authentication, authorization, and request proxying.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                           Clients                               │
│      (Mobile App, Web App, Admin Dashboard)                    │
└────────────────────────┬───────────────────────────────────────┘
                         │
                         ▼
                ┌────────────────┐
                │  API Gateway   │  ← Single Entry Point
                │   Port 21500   │  ← Authentication & Routing
                └───────┬────────┘
                        │
        ┌───────────────┼───────────────┬───────────────┐
        │               │               │               │
        ▼               ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ User Service │ │   Settings   │ │Zone Service  │ │Order Service │
│  Port 21501  │ │   Service    │ │ Port 21503   │ │ Port 21504   │
│ MySQL DB     │ │  Port 21502  │ │  MySQL DB    │ │  MySQL DB    │
│              │ │  MySQL DB    │ │              │ │  (Future)    │
└──────┬───────┘ └──────────────┘ └──────────────┘ └──────────────┘
       │
       ▼
┌──────────────┐
│  Keycloak    │  ← Identity & Access Management
│  Port 8080   │
└──────────────┘
```

## Services Overview

| Service | Port | Database | Stack | Description |
|---------|------|----------|-------|-------------|
| **API Gateway** | 21500 | - | Spring Boot (Java) | Entry point, authentication, request routing |
| **User Service** | 21501 | ds_user_service | Spring Boot (Java) | User management, Keycloak integration |
| **Settings Service** | 21502 | ds_settings_service | Spring Boot (Java) | System-wide configuration & secrets |
| **Zone Service** | 21503 | ds_zone_service | Node.js/Express | Zone & center management, spatial data |
| **Order Service** | 21504 | ds_order_service | Spring Boot (Java) | Order management (Future) |
| **Delivery Service** | 21505 | ds_delivery_service | - | Delivery tracking & management (Future) |

## Technology Stack

### Common Technologies
- **Authentication**: Keycloak (OAuth 2.0 / OpenID Connect)
- **Database**: MySQL 8.0+
- **API Documentation**: OpenAPI/Swagger
- **Environment**: Docker & Docker Compose
- **Version Control**: Git

### Service-Specific
- **Java Services**: Spring Boot 3.x, Spring Data JPA, Maven
- **Node.js Services**: Express.js, Prisma ORM, TypeScript, npm

## Quick Start

### Prerequisites

- Java 17+ (for Spring Boot services)
- Node.js 20+ (for Node.js services)
- MySQL 8.0+
- Docker & Docker Compose (optional, recommended)
- Maven 3.6+
- npm 10+

### 1. Setup Databases

#### Option A: Using SQL Script

```bash
cd BE
mysql -u root -p < scripts/init-databases.sql
```

#### Option B: Manual Creation

```bash
mysql -u root -p
```

```sql
CREATE DATABASE ds_user_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE ds_settings_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE ds_zone_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE ds_order_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE ds_delivery_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configure Environment

All services use environment variables from the root `env.local` file:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=root

# Database Names
USER_DB_NAME=ds_user_service
SETTINGS_DB_NAME=ds_settings_service
ZONE_DB_NAME=ds_zone_service
ORDER_DB_NAME=ds_order_service
DELIVERY_DB_NAME=ds_delivery_service

# Keycloak Configuration
KEYCLOAK_HOST=localhost
KEYCLOAK_PORT=8080
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_REALM_BACKEND=delivery-system-backend
KEYCLOAK_REALM_CLIENT=delivery-system-client

# Service URLs
API_GATEWAY_PORT=21500
USER_SERVICE_URL=http://localhost:21501
SETTINGS_SERVICE_URL=http://localhost:21502
ZONE_SERVICE_URL=http://localhost:21503

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,http://localhost:8080
```

### 3. Start Keycloak

Keycloak must be running before starting the services:

```bash
# Using Docker
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest \
  start-dev

# Or using Docker Compose
docker-compose up -d keycloak
```

Access Keycloak Admin Console: http://localhost:8080/admin

### 4. Start Services (Recommended Order)

The services have dependencies and should be started in this order:

#### Step 1: Start Settings Service

```bash
cd BE/Settings_service
mvn spring-boot:run
```

Wait for: `Settings Service started on port 21502`

#### Step 2: Start User Service

```bash
cd BE/User_service
mvn spring-boot:run
```

This will:
- Initialize Keycloak (realms, clients, roles, users)
- Create default admin user (username: `admin`, password: `admin`)
- Sync Keycloak settings to Settings Service

Wait for: `User Service started on port 21501`

#### Step 3: Start Zone Service

```bash
cd BE/zone_service
npm install
npm run prisma:generate
npm run prisma:migrate
npm run dev
```

Wait for: `Zone Service started on port 21503`

#### Step 4: Start API Gateway

```bash
cd BE/api-gateway
mvn spring-boot:run
```

Wait for: `API Gateway started on port 21500`

### 5. Verify Services

#### Health Checks

```bash
# API Gateway
curl http://localhost:21500/api/v1/health

# User Service
curl http://localhost:21501/api/v1/health

# Settings Service
curl http://localhost:21502/api/v1/health

# Zone Service
curl http://localhost:21503/api/v1/health
```

#### Test Authentication

```bash
# Login via API Gateway
curl -X POST http://localhost:21500/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin",
    "type": "BACKEND"
  }'
```

Response will include `access_token` and `refresh_token`.

## API Access Patterns

### Development (Direct Access)

Each service can be accessed directly:

- User Service: `http://localhost:21501/api/v1/users/...`
- Settings Service: `http://localhost:21502/api/v1/settings/...`
- Zone Service: `http://localhost:21503/api/v1/zones/...`

**Note**: Direct access bypasses authentication. Only use for development/testing.

### Production (Via Gateway)

All client requests should go through the API Gateway:

- User Routes: `http://localhost:21500/api/v1/users/...`
- Settings Routes: `http://localhost:21500/api/v1/settings/...`
- Zone Routes: `http://localhost:21500/api/v1/zones/...`
- Auth Routes: `http://localhost:21500/api/v1/auth/...`

The gateway handles:
- JWT token validation
- User context extraction
- Request routing to backend services
- CORS
- Error handling

## Authentication Flow

### 1. Login

```bash
POST /api/v1/auth/login
{
  "username": "admin",
  "password": "admin",
  "type": "BACKEND"
}
```

Response:
```json
{
  "result": {
    "access_token": "eyJhbGc...",
    "expires_in": 300,
    "refresh_token": "eyJhbGc...",
    "token_type": "Bearer"
  }
}
```

### 2. Use Access Token

Include the access token in the Authorization header:

```bash
curl -H "Authorization: Bearer eyJhbGc..." \
  http://localhost:21500/api/v1/users
```

### 3. Refresh Token

When the access token expires:

```bash
POST /api/v1/auth/refresh-token
{
  "refreshToken": "eyJhbGc..."
}
```

### 4. Logout

```bash
POST /api/v1/auth/logout
{
  "refreshToken": "eyJhbGc..."
}
```

## Settings Service Integration

### Settings Flow

1. **User Service** starts → Initializes Keycloak → Creates settings in Settings Service
2. **Other Services** can read/write settings via REST API
3. **Settings Format**: `AAA_BBBB_CCCC` (uppercase snake_case)

### Key Settings

| Setting Key | Service | Description |
|-------------|---------|-------------|
| `KEYCLOAK_AUTH_SERVER_URL` | User | Keycloak server URL |
| `KEYCLOAK_REALM_BACKEND` | User | Backend realm name |
| `KEYCLOAK_CLIENT_BACKEND_SECRET` | User | Backend client secret |
| `ZONE_MAX_RADIUS` | Zone | Max zone radius (future) |

### Access Settings

```bash
# Get all settings (requires auth)
GET /api/v1/settings

# Get public settings (no auth)
GET /api/v1/settings/public

# Get specific setting
GET /api/v1/settings/KEYCLOAK_AUTH_SERVER_URL

# Get setting value only
GET /api/v1/settings/KEYCLOAK_AUTH_SERVER_URL/value
```

## Docker Support

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Rebuild and start
docker-compose up -d --build
```

### Individual Service Docker

Each service has its own Dockerfile:

```bash
# Build
cd BE/User_service
docker build -t user-service .

# Run
docker run -p 21501:21501 --env-file ../env.local user-service
```

## API Documentation

### Swagger UI

Each service exposes Swagger UI for API documentation:

- API Gateway: http://localhost:21500/swagger-ui.html
- User Service: http://localhost:21501/swagger-ui.html
- Settings Service: http://localhost:21502/swagger-ui.html
- Zone Service: http://localhost:21503/api-docs (Swagger JSON)

### Route Documentation

Detailed route documentation is available in each service's `.docs/route/` folder:

- [API Gateway Routes](./api-gateway/.docs/route/) - [auth.md](./api-gateway/.docs/route/auth.md), [users.md](./api-gateway/.docs/route/users.md), [zones.md](./api-gateway/.docs/route/zones.md), [centers.md](./api-gateway/.docs/route/centers.md), [addresses.md](./api-gateway/.docs/route/addresses.md), [routing.md](./api-gateway/.docs/route/routing.md), [osrm.md](./api-gateway/.docs/route/osrm.md), [parcels.md](./api-gateway/.docs/route/parcels.md), [sessions.md](./api-gateway/.docs/route/sessions.md), [assignments.md](./api-gateway/.docs/route/assignments.md), [settings.md](./api-gateway/.docs/route/settings.md)
- [User Service Routes](./User_service/.docs/route/users.md)
- [Settings Service Routes](./Settings_service/.docs/route/settings.md)
- [Zone Service Routes](./zone_service/.docs/route/) - [zones.md](./zone_service/.docs/route/zones.md), [centers.md](./zone_service/.docs/route/centers.md), [addresses.md](./zone_service/.docs/route/addresses.md), [routing.md](./zone_service/.docs/route/routing.md), [osrm.md](./zone_service/.docs/route/osrm.md)
- [Communication Service Routes](./communication_service/.docs/route/) - [proposals.md](./communication_service/.docs/route/proposals.md), [conversations.md](./communication_service/.docs/route/conversations.md), [proposal-configs.md](./communication_service/.docs/route/proposal-configs.md)
- [Session Service Routes](./session-service/.docs/route/) - [sessions.md](./session-service/.docs/route/sessions.md), [assignments.md](./session-service/.docs/route/assignments.md)
- [Parcel Service Routes](./parcel-service/.docs/route/parcels.md)

## Service Documentation

Each service has comprehensive README documentation:

- [API Gateway](./api-gateway/README.md) - Entry point, authentication, routing
- [User Service](./User_service/README.MD) - User management, Keycloak integration
- [Settings Service](./Settings_service/README.md) - System configuration management
- [Zone Service](./zone_service/README.md) - Zone & center management

## Common Response Format

All services follow the `BaseResponse<T>` contract:

### Success Response

```json
{
  "result": { ... },
  "message": "Operation successful"
}
```

### Paginated Response

```json
{
  "result": {
    "data": [ ... ],
    "page": {
      "page": 0,
      "size": 10,
      "totalElements": 100,
      "totalPages": 10
    }
  }
}
```

### Error Response

```json
{
  "message": "Error description",
  "errors": [
    {
      "field": "fieldName",
      "message": "Validation error"
    }
  ]
}
```

## Development Tools

### Java Services

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# Test
mvn test

# Skip tests
mvn clean package -DskipTests
```

### Node.js Services

```bash
# Install dependencies
npm install

# Development mode
npm run dev

# Production build
npm run build
npm start

# Run tests
npm test

# Prisma commands
npm run prisma:generate
npm run prisma:migrate
npm run prisma:studio
```

## Monitoring & Logs

### Log Locations

- **Java Services**: Console output (Spring Boot logging)
- **Zone Service**: `./zone_service/logs/` directory
- **Docker**: `docker-compose logs -f [service-name]`

### Health Checks

All services expose health endpoints:

```bash
GET /api/v1/health        # Basic health
GET /health/detailed      # Detailed health (Zone Service)
GET /actuator/health      # Spring Actuator (Java services)
```

## Troubleshooting

### Services Won't Start

1. **Check Keycloak**: Ensure Keycloak is running on port 8080
2. **Check MySQL**: Verify MySQL is running and databases are created
3. **Check Ports**: Ensure ports 21500-21503 are not in use
4. **Check Dependencies**: User Service needs Settings Service running first

### Database Connection Issues

```bash
# Test MySQL connection
mysql -u root -p -h localhost -P 3306

# Check database exists
SHOW DATABASES LIKE 'ds_%';

# Verify credentials in env.local
```

### Keycloak Issues

```bash
# Check Keycloak is running
curl http://localhost:8080

# Access admin console
http://localhost:8080/admin
Username: admin
Password: admin

# Check logs
docker logs keycloak
```

### Authentication Fails

1. **Check Keycloak is initialized**: User Service should create realms/clients
2. **Check credentials**: Default is `admin`/`admin`
3. **Check token expiration**: Tokens expire after 5 minutes (configurable)
4. **Refresh token**: Use refresh token endpoint

### Service Communication Issues

1. **Verify service URLs in env.local**
2. **Check all services are running**
3. **Test service health endpoints**
4. **Check network connectivity** (especially in Docker)

## Testing

### Manual Testing with cURL

See examples in each service's documentation.

### Automated Testing

```bash
# Java services
mvn test

# Node.js services
npm test

# Integration tests
mvn verify
```

### Postman Collection

Import the Postman collection (if available) for comprehensive API testing.

## Production Considerations

### Security

1. **Change default passwords**: Keycloak admin, database passwords
2. **Use HTTPS**: Enable SSL/TLS for all services
3. **Secure secrets**: Use proper secret management (not env.local)
4. **Enable authentication**: Never expose services directly without auth
5. **Update CORS**: Configure proper CORS origins for production

### Performance

1. **Database connection pooling**: Configure in application.yaml
2. **Caching**: Settings Service uses caching (TTL configurable)
3. **Load balancing**: Use load balancer in front of API Gateway
4. **Horizontal scaling**: Services are stateless and can scale horizontally

### Monitoring

1. **Enable Spring Actuator**: For metrics and health checks
2. **Centralized logging**: Use ELK stack or similar
3. **APM tools**: Consider New Relic, DataDog, etc.
4. **Database monitoring**: Monitor query performance

### Deployment

1. **Use Docker Compose** or Kubernetes for orchestration
2. **CI/CD pipeline**: Automate builds and deployments
3. **Blue-green deployment**: For zero-downtime updates
4. **Database migrations**: Handle Flyway/Prisma migrations carefully

## Best Practices

1. **Always use API Gateway** for client requests
2. **Follow RESTful standards** (see `RESTFUL.md`)
3. **Use proper HTTP status codes**
4. **Implement proper error handling**
5. **Validate input data** using DTOs and validators
6. **Document all APIs** in Swagger/OpenAPI
7. **Write tests** for critical functionality
8. **Keep services independent** (loose coupling)
9. **Use environment variables** for configuration
10. **Follow naming conventions** (see service documentation)

## Additional Documentation

- [Global RESTful Standards](./RESTFUL.md)
- [Keycloak Migration Guide](./api-gateway/KEYCLOAK_MIGRATION.md)
- [Database Initialization Script](./scripts/init-databases.sql)

## Support & Contributing

For issues, questions, or contributions:
1. Check service-specific README files
2. Review `.docs` folders for detailed route documentation
3. Check Swagger UI for API specifications
4. Review error logs for debugging

## Summary

The Backend Services provide a complete microservices architecture for the Delivery System:

- **API Gateway**: Single entry point with authentication & routing
- **User Service**: User management & Keycloak integration
- **Settings Service**: Centralized configuration management
- **Zone Service**: Geographical zone & center management
- **Keycloak**: Identity & access management

All services are:
- RESTful and stateless
- Independently deployable
- Well-documented
- Production-ready
- Docker-compatible

**Base URL (Production)**: `http://localhost:21500/api/v1`
**Authentication**: Keycloak (OAuth 2.0 / OpenID Connect)
**Documentation**: Swagger UI + Markdown docs
