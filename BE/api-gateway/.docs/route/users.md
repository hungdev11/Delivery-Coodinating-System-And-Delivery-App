# User Routes

Base URL: `http://localhost:8080/api/v1`

All endpoints require authentication via JWT token.

## Endpoints

### GET /users/me
- Description: Get current authenticated user (requires auth).
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
    "phone": "+84123456789",
    "address": "123 Main St",
    "identityNumber": "0123456789",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

### GET /users
- Description: List all users (requires ADMIN or MANAGER role).
- Response 200:
```json
{
  "result": [
    {
      "id": "uuid",
      "username": "johndoe",
      "email": "john@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "status": "ACTIVE"
    }
  ]
}
```

### GET /users/:id
- Description: Get user by ID (requires auth).
- Response 200: Same as GET /users/me

### GET /users/username/:username
- Description: Get user by username (requires auth).
- Response 200: Same as GET /users/me

### POST /users
- Description: Create user (requires ADMIN role).
- Body:
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+84123456789",
  "address": "123 Main St",
  "identityNumber": "0123456789"
}
```
- Response 200:
```json
{
  "result": { ... },
  "message": "User created successfully"
}
```

### PUT /users/:id
- Description: Update user (requires auth).
- Body: Same as POST /users (all fields optional)
- Response 200:
```json
{
  "result": { ... },
  "message": "User updated successfully"
}
```

### DELETE /users/:id
- Description: Delete user (requires ADMIN role).
- Response 200:
```json
{
  "result": null,
  "message": "User deleted successfully"
}
```

## Authorization

- **Public**: No authentication required
- **Authenticated**: Requires valid JWT token
- **ADMIN**: Requires ADMIN role in Keycloak
- **MANAGER**: Requires MANAGER role in Keycloak
