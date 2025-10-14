# Authentication Routes

Base URL: `http://localhost:8080/api/v1`

## Endpoints

### POST /auth/login
- Description: Login with username/password via Keycloak (public).
- Body:
```json
{
  "username": "johndoe",
  "password": "password123",
  "type": "BACKEND"
}
```
- Types: `BACKEND` (admin/staff), `FRONTEND` (shipper/client)
- Response 200:
```json
{
  "result": {
    "message": "Login successful",
    "access_token": "eyJhbGc...",
    "expires_in": 300,
    "refresh_expires_in": 1800,
    "refresh_token": "eyJhbGc...",
    "token_type": "Bearer",
    "user": {
      "id": "uuid",
      "keycloakId": "keycloak-uuid",
      "username": "johndoe",
      "email": "john@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "phone": "+84123456789",
      "address": "123 Main St",
      "identityNumber": "0123456789",
      "status": "ACTIVE",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  },
  "message": "Login successful"
}
```
- Errors: 400 invalid credentials.

### POST /auth/login/default
- Description: Login using default realm and client (public).
- Body: Same as POST /auth/login (without `type` field)
- Response 200: Same as POST /auth/login

### POST /auth/login/custom
- Description: Login with specific realm and client (public).
- Body:
```json
{
  "username": "johndoe",
  "password": "password123",
  "realm": "custom-realm",
  "clientId": "custom-client"
}
```
- Response 200: Same as POST /auth/login

### POST /auth/validate-token
- Description: Validate JWT token and get user info (public).
- Headers: `Authorization: Bearer <token>`
- Response 200:
```json
{
  "result": {
    "sub": "keycloak-user-id",
    "preferred_username": "johndoe",
    "email": "john@example.com",
    "given_name": "John",
    "family_name": "Doe",
    "realm_access": {
      "roles": ["ADMIN", "USER"]
    }
  },
  "message": "Token valid"
}
```
- Errors: 400 invalid token.

### POST /auth/refresh-token
- Description: Refresh access token (public).
- Body:
```json
{
  "refreshToken": "eyJhbGc..."
}
```
- Response 200: Same as POST /auth/login

### POST /auth/logout
- Description: Logout by invalidating refresh token (public).
- Body:
```json
{
  "refreshToken": "eyJhbGc..."
}
```
- Response 200:
```json
{
  "result": true,
  "message": "Logout successful"
}
```

### GET /auth/me
- Description: Get current authenticated user info (requires auth).
- Headers: `Authorization: Bearer <token>`
- Response 200:
```json
{
  "result": {
    "sub": "keycloak-user-id",
    "preferred_username": "johndoe",
    "email": "john@example.com",
    "given_name": "John",
    "family_name": "Doe",
    "realm_access": {
      "roles": ["ADMIN", "USER"]
    }
  },
  "message": "User info retrieved"
}
```
- Errors: 400 authentication required.

### POST /auth/sync
- Description: Sync user from Keycloak to User Service (requires auth).
- Headers: `Authorization: Bearer <token>`
- Response 200:
```json
{
  "result": {
    "id": "uuid",
    "keycloakId": "keycloak-uuid",
    "username": "johndoe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "status": "ACTIVE"
  },
  "message": "User synced successfully"
}
```
- Note: This creates or updates the user in the User Service database.
