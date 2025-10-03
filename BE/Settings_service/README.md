# Settings Service

## 📋 Tổng quan

Settings Service quản lý cấu hình và secrets của toàn bộ hệ thống Delivery System.

## 🎯 Chức năng

- ✅ Quản lý system-wide settings
- ✅ Lưu trữ configuration (Keycloak, Email, Payment, etc.)
- ✅ Caching để optimize performance
- ✅ REST API đầy đủ (CRUD)
- ✅ Read-only settings protection
- ✅ Multi-level settings (SYSTEM, APPLICATION, SERVICE, FEATURE, USER)
- ✅ Auto-initialized by other services

## 🗄️ Database Schema

### Table: `system_settings`

| Column | Type | Description |
|--------|------|-------------|
| `setting_key` | VARCHAR(100) | Primary key - Format: AAA_BBBB_CCCC |
| `setting_group` | VARCHAR(50) | Group name (keycloak, email, payment) |
| `description` | VARCHAR(500) | Setting description |
| `value_type` | ENUM | STRING, INTEGER, DECIMAL, BOOLEAN, JSON |
| `setting_value` | TEXT | Actual value |
| `level` | ENUM | SYSTEM, APPLICATION, SERVICE, FEATURE, USER |
| `is_read_only` | BOOLEAN | Cannot be modified if true |
| `display_mode` | ENUM | TEXT, PASSWORD, CODE, NUMBER, TOGGLE, TEXTAREA, URL, EMAIL |
| `created_at` | TIMESTAMP | Created timestamp |
| `updated_at` | TIMESTAMP | Last updated timestamp |
| `updated_by` | VARCHAR(100) | Last updated by user |

## 🔧 Configuration

### Environment Variables (from root `env.local`)

```bash
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=root
SETTINGS_DB_NAME=ds_settings_service
```

All configs automatically loaded from master env.

### Application Settings (`application.yaml`)

```yaml
server:
  port: 21502

settings:
  cache:
    ttl-minutes: 60
```

## 📊 Settings Examples

### Key Naming Convention
Format: `AAA_BBBB_CCCC` (snake_case uppercase)

Examples:
- `KEYCLOAK_AUTH_SERVER_URL`
- `EMAIL_SMTP_HOST`
- `PAYMENT_GATEWAY_URL`
- `APP_MAINTENANCE_MODE`

### Settings are managed by:
- **User Service**: Keycloak-related settings (auto-initialized on startup)
- **Other Services**: Their own configurations
- **Settings Service**: Provides storage and API only

### Example Settings Created by User Service:
```
KEYCLOAK_HOST                    = localhost
KEYCLOAK_PORT                    = 8080
KEYCLOAK_REALM_BACKEND           = keycloak
KEYCLOAK_REALM_CLIENT            = delivery-system-client
KEYCLOAK_CLIENT_BACKEND_ID       = delivery-backend
KEYCLOAK_CLIENT_BACKEND_SECRET   = 46sGOyba6nyt8UhLkKAQgzmbedF9L042
KEYCLOAK_CLIENT_WEB_ID           = delivery-management-web
KEYCLOAK_CLIENT_MOBILE_ID        = delivery-mobile-app
KEYCLOAK_AUTH_SERVER_URL         = http://localhost:8080
```

## 🚀 API Endpoints

### Get Settings
```bash
# Get all settings
GET /api/v1/settings

# Get setting by key
GET /api/v1/settings/{key}

# Get setting value only
GET /api/v1/settings/{key}/value

# Get settings by group
GET /api/v1/settings/group/{group}

# Get settings by level
GET /api/v1/settings/level/{level}

# Get public settings (for clients)
GET /api/v1/settings/public

# Search settings
GET /api/v1/settings/search?q=keycloak
```

### Create Setting
```bash
POST /api/v1/settings
Content-Type: application/json

{
  "key": "my.setting.key",
  "group": "mygroup",
  "description": "My setting description",
  "type": "STRING",
  "value": "my value",
  "level": "APPLICATION",
  "isReadOnly": false,
  "isEncrypted": false,
  "isPublic": false
}
```

### Update Setting
```bash
PUT /api/v1/settings/{key}
Content-Type: application/json
X-User-Id: admin

{
  "description": "Updated description",
  "value": "updated value",
  "isPublic": true
}
```

### Delete Setting
```bash
DELETE /api/v1/settings/{key}
```

## 🔐 Security Features

### 1. Read-Only Settings
- Settings có `isReadOnly=true` không thể update/delete
- Protect critical system settings

### 3. Public vs Private
- `isPublic=true`: Client có thể access
- `isPublic=false`: Chỉ backend services access được

### 4. Setting Levels
- `SYSTEM`: Highest priority, system-wide
- `APPLICATION`: Application-level
- `SERVICE`: Service-specific
- `FEATURE`: Feature-specific
- `USER`: User-level (lowest priority)

## 💾 Cache Strategy

- Settings được cache với TTL 60 minutes (configurable)
- Cache keys:
  - `settings` - Individual settings by key
  - `settingsByGroup` - Settings by group
- Cache được clear khi CREATE/UPDATE/DELETE

## 📚 Usage Examples

### Get Setting Example
```bash
curl http://localhost:21502/api/v1/settings/KEYCLOAK_CLIENT_SECRET
```

Response:
```json
{
  "key": "KEYCLOAK_CLIENT_SECRET",
  "group": "keycloak",
  "description": "Backend service client secret",
  "type": "STRING",
  "value": "46sGOyba6nyt8UhLkKAQgzmbedF9L042",
  "level": "SYSTEM",
  "isReadOnly": true
}
```

### Get All Keycloak Settings
```bash
curl http://localhost:21502/api/v1/settings/group/keycloak
```

## 🛠️ Development

### Run Application
```bash
cd BE/Settings_service
mvn spring-boot:run
```

### Create Database
```sql
CREATE DATABASE settings_service 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

### Access Swagger UI
```
http://localhost:21502/swagger-ui.html
```

## 📝 Best Practices

1. **Naming**: Use `AAA_BBBB_CCCC` format (snake_case uppercase)
2. **Read-Only**: Set `isReadOnly=true` for critical system settings
3. **Groups**: Group related settings (keycloak, email, payment)
4. **Levels**: Use appropriate level (SYSTEM for critical, APPLICATION for app-wide)
5. **Service Ownership**: Each service manages its own settings

## 🔄 Integration with Other Services

### User Service
```java
// Get setting via REST call
String keycloakUrl = restTemplate.getForObject(
    "http://settings-service:21502/api/v1/settings/keycloak.auth.server.url/value",
    String.class
);
```

### API Gateway
- Forward requests to `/api/v1/settings/public` for client access
- Protect private settings endpoints with authentication

## 🎉 Summary

Settings Service cung cấp:
- ✅ Centralized configuration management
- ✅ Secure secrets storage với encryption
- ✅ Flexible access control (public/private, read-only)
- ✅ Multi-level settings hierarchy
- ✅ Caching for performance
- ✅ RESTful API đầy đủ
- ✅ Auto-initialization với default settings

---
**Port**: 21502
**Database**: settings_service
**Package**: com.ds.setting
