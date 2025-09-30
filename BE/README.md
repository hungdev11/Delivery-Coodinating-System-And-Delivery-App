# Backend Services - Quick Start

## 📋 Services

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| API Gateway | 21500 | - | Entry point |
| User Service | 21501 | ds_user_service | User management + Keycloak init |
| Settings Service | 21502 | ds_settings_service | System settings + secrets |
| Order Service | 21503 | ds_order_service | (Future) |
| Delivery Service | 21504 | ds_delivery_service | (Future) |

## 🚀 Quick Start

### 1. Create Databases
```bash
# Run SQL script
mysql -u root -p < create-databases.sql

# Or manually
mysql -u root -p
```
```sql
CREATE DATABASE ds_user_service;
CREATE DATABASE ds_settings_service;
CREATE DATABASE ds_order_service;
CREATE DATABASE ds_delivery_service;
```

### 2. Run Services
Each service sử dụng env từ `env.local` (root):

```bash
# Terminal 1 - User Service
cd BE/User_service
mvn spring-boot:run

# Terminal 2 - Settings Service
cd BE/Settings_service
mvn spring-boot:run

# Terminal 3 - API Gateway
cd BE/api-gateway
mvn spring-boot:run
```

## 🔧 Environment

Tất cả config trong **`env.local`** (root):
- Database: localhost:3306 (root/root)
- Keycloak: localhost:8080 (dev/dev)
- Services: localhost:215XX

## 📚 API Access

### Direct (Development)
- User: http://localhost:21501/api/v1/...
- Settings: http://localhost:21502/api/v1/...

### Via Gateway (Production-like)
- User: http://localhost:21500/api/v1/users/...
- Settings: http://localhost:21500/api/v1/settings/...
  - Public: http://localhost:21500/api/v1/settings/public

## 🔑 Settings Flow

1. **User Service** khởi động → Init Keycloak → Tạo settings trong Settings Service
2. **Other Services** có thể create/read settings qua API
3. Settings format: `AAA_BBBB_CCCC` (uppercase snake_case)

Example:
```bash
# Get Keycloak client secret
GET /api/v1/settings/KEYCLOAK_CLIENT_BACKEND_SECRET
```

## 📖 Documentation

- User Service: `User_service/README.MD`
- Settings Service: `Settings_service/README.md`
- Keycloak Init: `User_service/KEYCLOAK_INIT_GUIDE.md`
