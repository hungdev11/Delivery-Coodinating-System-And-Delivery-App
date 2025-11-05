# User Service

## Overview

The User Service manages user accounts, profiles, and integrates with Keycloak for authentication and identity management. It serves as the central user data store and synchronization point between Keycloak and the application database.

## Architecture

### Technology Stack

- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Database**: MySQL 8.0+ (`ds_user_service`)
- **ORM**: Spring Data JPA with Hibernate
- **Authentication**: Keycloak Admin Client SDK
- **Migration**: Flyway
- **Build Tool**: Maven

### Key Responsibilities

- User CRUD operations
- Keycloak user synchronization
- User profile management
- Integration with Settings Service
- Keycloak realm and client initialization
- User authentication state management

## Port Configuration

- **Development**: `21501`
- **Base URL**: `http://localhost:21501/api/v1`

## Database

### Database Name
`ds_user_service`

### Main Tables

#### `users` Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `keycloak_id` | VARCHAR(255) | Keycloak user ID (unique) |
| `username` | VARCHAR(100) | Username (unique) |
| `email` | VARCHAR(255) | Email address (unique) |
| `first_name` | VARCHAR(100) | First name |
| `last_name` | VARCHAR(100) | Last name |
| `phone_number` | VARCHAR(20) | Phone number |
| `status` | VARCHAR(20) | ACTIVE, INACTIVE, SUSPENDED |
| `created_at` | TIMESTAMP | Created timestamp |
| `updated_at` | TIMESTAMP | Last updated timestamp |
| `created_by` | VARCHAR(100) | Creator |
| `updated_by` | VARCHAR(100) | Last updater |

### Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`

```bash
# Migrations run automatically on startup
V1__init.sql  # Initial schema
```

## Environment Configuration

### Environment Variables (from root `env.local`)

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=root
USER_DB_NAME=ds_user_service

# Keycloak Configuration
KEYCLOAK_HOST=localhost
KEYCLOAK_PORT=8080
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_REALM_BACKEND=delivery-system-backend
KEYCLOAK_REALM_CLIENT=delivery-system-client

# Settings Service Configuration
SETTINGS_SERVICE_URL=http://localhost:21502
```

### Application Settings (`application.yaml`)

```yaml
server:
  port: 21501

spring:
  application:
    name: user-service
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${USER_DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    baseline-on-migrate: true

keycloak:
  server-url: http://${KEYCLOAK_HOST}:${KEYCLOAK_PORT}
  admin:
    username: ${KEYCLOAK_ADMIN_USERNAME}
    password: ${KEYCLOAK_ADMIN_PASSWORD}
```

## Features

### 1. User Management

- Create, read, update, delete (CRUD) operations
- User profile management
- User status management (ACTIVE, INACTIVE, SUSPENDED)
- Pagination and search capabilities
- Email and username uniqueness validation

### 2. Keycloak Integration

#### Automatic Initialization on Startup

The service automatically initializes Keycloak resources when it starts:

1. **Master Realm Connection**
   - Connects to Keycloak master realm
   - Authenticates with admin credentials

2. **Realm Initialization**
   - Creates backend realm (`delivery-system-backend`)
   - Creates client realm (`delivery-system-client`)
   - Configures realm settings

3. **Client Initialization**
   - Creates `delivery-backend` client (confidential)
   - Creates `delivery-management-web` client (public)
   - Creates `delivery-mobile-app` client (public)
   - Configures client settings (redirect URIs, CORS, etc.)

4. **Role Initialization**
   - Creates ADMIN role
   - Creates USER role
   - Creates SHIPPER role
   - Creates CUSTOMER role

5. **Default User Creation**
   - Creates admin user with ADMIN role
   - Sets default password
   - Enables user account

6. **Settings Synchronization**
   - Pushes Keycloak configuration to Settings Service
   - Stores realm names, client IDs, secrets, etc.

See [Keycloak Initialization Services](./src/main/java/com/ds/user/application/startup/data/services/README.md) for implementation details.

### 3. User Synchronization

The service provides endpoints to sync users between Keycloak and the application database:

- Sync user from Keycloak to User Service (create or update)
- Retrieve user information from Keycloak
- Map Keycloak attributes to User entity

### 4. Settings Integration

The service integrates with Settings Service to:
- Store Keycloak configuration (realm names, client IDs, secrets)
- Initialize default system settings
- Provide configuration to other services

## API Endpoints

### User CRUD Operations

#### Get All Users (Paginated)

```bash
GET /api/v1/users?page=0&size=10&search=john
```

Response:
```json
{
  "result": {
    "data": [
      {
        "id": "uuid",
        "keycloakId": "keycloak-uuid",
        "username": "johndoe",
        "email": "john@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "phoneNumber": "+1234567890",
        "status": "ACTIVE",
        "createdAt": "2025-01-01T00:00:00Z",
        "updatedAt": "2025-01-01T00:00:00Z"
      }
    ],
    "page": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  }
}
```

#### Get User by ID

```bash
GET /api/v1/users/{id}
```

#### Get User by Keycloak ID

```bash
GET /api/v1/users/keycloak/{keycloakId}
```

#### Get User by Username

```bash
GET /api/v1/users/username/{username}
```

#### Create User

```bash
POST /api/v1/users
Content-Type: application/json

{
  "keycloakId": "keycloak-uuid",
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "status": "ACTIVE"
}
```

Response:
```json
{
  "result": {
    "id": "uuid",
    "keycloakId": "keycloak-uuid",
    "username": "johndoe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890",
    "status": "ACTIVE"
  },
  "message": "User created successfully"
}
```

#### Update User

```bash
PUT /api/v1/users/{id}
Content-Type: application/json
X-User-Id: admin

{
  "firstName": "John Updated",
  "lastName": "Doe Updated",
  "phoneNumber": "+1234567891",
  "status": "ACTIVE"
}
```

#### Delete User

```bash
DELETE /api/v1/users/{id}
```

Response:
```json
{
  "result": null,
  "message": "User deleted successfully"
}
```

### Authentication & Sync Endpoints

#### Sync User from Keycloak

```bash
POST /api/v1/users/sync
Content-Type: application/json

{
  "keycloakId": "keycloak-uuid",
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

This endpoint:
- Creates user if not exists (by keycloakId)
- Updates user if already exists
- Returns the synchronized user

### Health Check

```bash
GET /api/v1/health
```

Response:
```json
{
  "status": "UP",
  "message": "User Service is running"
}
```

## Response Format

All responses follow the `BaseResponse<T>` contract:

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
      "field": "email",
      "message": "Email already exists"
    }
  ]
}
```

## Development

### Prerequisites

- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Keycloak 21+ running on port 8080
- Settings Service running on port 21502

### Setup Database

```bash
# Connect to MySQL
mysql -u root -p

# Create database
CREATE DATABASE ds_user_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Or use the initialization script
mysql -u root -p < ../scripts/init-databases.sql
```

### Run Application

```bash
cd BE/User_service
mvn spring-boot:run
```

The service will:
1. Connect to database
2. Run Flyway migrations
3. Initialize Keycloak (realms, clients, roles, users)
4. Sync settings to Settings Service
5. Start HTTP server on port 21501

### Build Application

```bash
mvn clean package
```

The JAR will be created in `target/User_service-*.jar`

### Run Tests

```bash
mvn test
```

### Access Swagger UI

```
http://localhost:21501/swagger-ui.html
```

## Keycloak Initialization Flow

The service performs the following initialization on startup:

### 1. Master Realm Connection
Connects to Keycloak master realm using admin credentials.

### 2. Realm Creation
Creates two realms if they don't exist:
- `delivery-system-backend` - For admin/staff users
- `delivery-system-client` - For shipper/customer users

### 3. Client Creation

**Backend Realm Clients:**
- `delivery-backend` (confidential) - For backend services

**Client Realm Clients:**
- `delivery-management-web` (public) - For management web app
- `delivery-mobile-app` (public) - For mobile app

### 4. Role Creation
Creates roles in both realms:
- ADMIN
- USER
- SHIPPER
- CUSTOMER

### 5. Default User Creation
Creates default admin user:
- Username: `admin`
- Password: `admin` (change in production!)
- Roles: ADMIN, USER

### 6. Settings Synchronization
Pushes Keycloak configuration to Settings Service:
- Realm names
- Client IDs
- Client secrets
- Auth server URLs

All settings are stored with the `keycloak` group and marked as read-only.

## Settings Created by User Service

The service creates the following settings in Settings Service:

| Setting Key | Description | Example Value |
|-------------|-------------|---------------|
| `KEYCLOAK_HOST` | Keycloak server host | `localhost` |
| `KEYCLOAK_PORT` | Keycloak server port | `8080` |
| `KEYCLOAK_REALM_BACKEND` | Backend realm name | `delivery-system-backend` |
| `KEYCLOAK_REALM_CLIENT` | Client realm name | `delivery-system-client` |
| `KEYCLOAK_CLIENT_BACKEND_ID` | Backend client ID | `delivery-backend` |
| `KEYCLOAK_CLIENT_BACKEND_SECRET` | Backend client secret | Generated UUID |
| `KEYCLOAK_CLIENT_WEB_ID` | Web client ID | `delivery-management-web` |
| `KEYCLOAK_CLIENT_MOBILE_ID` | Mobile client ID | `delivery-mobile-app` |
| `KEYCLOAK_AUTH_SERVER_URL` | Full Keycloak URL | `http://localhost:8080` |

All settings are:
- Group: `keycloak`
- Level: `SYSTEM`
- Read-only: `true`
- Type: `STRING`

## Integration with Other Services

### Settings Service
- Stores Keycloak configuration
- Provides configuration to other services
- Settings Service must be running before User Service starts

### API Gateway
- Gateway proxies requests to User Service
- Gateway validates JWT tokens from Keycloak
- Gateway syncs users after authentication

## Docker Support

### Build Docker Image

```bash
docker build -t user-service .
```

### Run Container

```bash
docker run -p 21501:21501 \
  --env-file ../env.local \
  user-service
```

### Docker Compose

The service is included in the main `docker-compose.yml`:

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f user-service
```

## Troubleshooting

### Cannot connect to Keycloak

- Verify Keycloak is running on configured host/port
- Check `KEYCLOAK_HOST` and `KEYCLOAK_PORT` in env.local
- Verify admin credentials are correct
- Check Keycloak logs for errors

### Database connection failed

- Verify MySQL is running
- Check database name, username, password
- Ensure database is created
- Run: `CREATE DATABASE ds_user_service;`

### Settings Service not available

- Verify Settings Service is running on port 21502
- Check `SETTINGS_SERVICE_URL` in env.local
- The service will retry connection on startup

### Flyway migration fails

```bash
# Clean migrations (WARNING: deletes all data)
mvn flyway:clean

# Re-run migrations
mvn flyway:migrate
```

### Keycloak initialization fails

- Check Keycloak admin credentials
- Verify Keycloak is fully started
- Check Keycloak logs for errors
- Ensure realms don't already exist with different configs

## Best Practices

1. **Change default admin password** in production
2. **Use environment variables** for sensitive configuration
3. **Enable SSL/TLS** for Keycloak in production
4. **Backup database** regularly
5. **Monitor Keycloak sync** for errors
6. **Use strong passwords** for Keycloak admin
7. **Implement proper role-based access control**

## Documentation

- [API Documentation](.docs/README.md)
- [Route Documentation](.docs/route/users.md)
- [Authentication Interface](./src/main/java/com/ds/user/common/interfaces/README_AUTHENTICATION.md)
- [Keycloak Initialization Services](./src/main/java/com/ds/user/application/startup/data/services/README.md)
- [Global RESTful Standards](../../RESTFUL.md)

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

Test coverage includes:
- User CRUD operations
- Keycloak synchronization
- Repository operations
- Controller endpoints

## Summary

The User Service provides:
- Centralized user management
- Keycloak integration and initialization
- User synchronization between Keycloak and database
- RESTful API for user operations
- Automatic realm, client, role, and user setup
- Settings integration for configuration sharing

**Port**: 21501
**Database**: ds_user_service
**Package**: com.ds.user
**Main Class**: `UserServiceApplication.java`
