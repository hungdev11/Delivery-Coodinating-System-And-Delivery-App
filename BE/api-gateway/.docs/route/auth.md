# Authentication Routes (API Gateway)

Base URL: `http://localhost:<port>/api/v1/auth`

**Note:** These endpoints are handled directly by the API Gateway for authentication.

## Endpoints

### POST /auth/login
- Description: Login with username/password via Keycloak.
- Body:
```json
{
  "username": "testuser",
  "password": "password",
  "type": "BACKEND"
}
```
- Response 200:
```json
{
  "status": "success",
  "message": "Login successful",
  "data": {
    "accessToken": "jwt-token",
    "refreshToken": "refresh-token",
    "tokenType": "Bearer",
    "expiresIn": 300,
    "user": {
      "id": "uuid",
      "username": "testuser",
      "email": "test@example.com"
    }
  }
}
```

### POST /auth/login/default
- Description: Login with default realm/client.
- Body: Same as POST /auth/login
- Response 200: Same as POST /auth/login

### POST /auth/login/custom
- Description: Login with custom realm/client.
- Body:
```json
{
  "username": "testuser",
  "password": "password",
  "realm": "realm-name",
  "clientId": "client-id"
}
```
- Response 200: Same as POST /auth/login

### POST /auth/validate-token
- Description: Validate JWT token.
- Headers: `Authorization: Bearer <token>`
- Response 200:
```json
{
  "status": "success",
  "message": "Token valid",
  "data": {
    "sub": "keycloak-user-id",
    "preferredUsername": "testuser",
    "email": "test@example.com"
  }
}
```

### POST /auth/refresh-token
- Description: Refresh access token.
- Body:
```json
{
  "refreshToken": "refresh-token"
}
```
- Response 200:
```json
{
  "status": "success",
  "message": "Token refreshed",
  "data": {
    "accessToken": "new-jwt-token",
    "refreshToken": "new-refresh-token",
    "tokenType": "Bearer",
    "expiresIn": 300
  }
}
```

### POST /auth/logout
- Description: Logout and invalidate refresh token.
- Body:
```json
{
  "refreshToken": "refresh-token"
}
```
- Response 200:
```json
{
  "status": "success",
  "message": "Logout successful",
  "data": true
}
```

### GET /auth/me
- Description: Get current user info from JWT token.
- Headers: `Authorization: Bearer <token>`
- Response 200:
```json
{
  "status": "success",
  "message": "User info retrieved",
  "data": {
    "sub": "keycloak-user-id",
    "preferredUsername": "testuser",
    "email": "test@example.com"
  }
}
```

### POST /auth/sync
- Description: Sync user data from Keycloak to User Service.
- Headers: `Authorization: Bearer <token>`
- Response 200:
```json
{
  "status": "success",
  "message": "User synced successfully",
  "data": {
    "id": "uuid",
    "username": "testuser",
    "email": "test@example.com"
  }
}
```
