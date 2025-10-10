# User Routes

Base URL: `http://localhost:8081/api/v1`

## Endpoints

### GET /users
- Description: Get all users.
- Response 200:
```json
{
  "result": [
    {
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
  ]
}
```

### GET /users/:id
- Description: Get user by ID.
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
- Response 404:
```json
{
  "message": "User not found"
}
```

### GET /users/username/:username
- Description: Get user by username.
- Response 200: Same as GET /users/:id
- Response 404: Same as GET /users/:id

### GET /users/keycloak/:keycloakId
- Description: Get user by Keycloak ID.
- Response 200 same as GET /users/:id
- Response 404 same as GET /users/:id

### POST /users
- Description: Create a new user.
- Body:
```json
{
  "keycloakId": "optional-keycloak-uuid",
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+84123456789",
  "address": "123 Main St",
  "identityNumber": "0123456789",
  "status": "ACTIVE"
}
```
- Response 201:
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
    "updatedAt": null
  },
  "message": "User created successfully"
}
```
- Errors: 400 validation failed.

### PUT /users/:id
- Description: Update user.
- Body (all fields optional):
```json
{
  "email": "newemail@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "phone": "+84987654321",
  "address": "456 Another St",
  "identityNumber": "9876543210",
  "status": "BLOCKED"
}
```
- Response 200:
```json
{
  "result": {
    "id": "uuid",
    "keycloakId": "keycloak-uuid",
    "username": "johndoe",
    "email": "newemail@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "phone": "+84987654321",
    "address": "456 Another St",
    "identityNumber": "9876543210",
    "status": "BLOCKED",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-02T00:00:00"
  },
  "message": "User updated successfully"
}
```
- Errors: 404 user not found.

### DELETE /users/:id
- Description: Delete user.
- Response 200:
```json
{
  "result": null,
  "message": "User deleted successfully"
}
```

### POST /users/sync
- Description: Sync user from Keycloak (create or update).
- Body:
```json
{
  "keycloakId": "keycloak-uuid",
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```
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
    "phone": null,
    "address": null,
    "identityNumber": null,
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  },
  "message": "User synced successfully"
}
```
- Note: This endpoint creates a new user if keycloakId doesn't exist, or updates existing user's fields.

## User Status

User status can be one of:
- `ACTIVE` (1) - User is active and can use the system
- `PENDING` (2) - User registration is pending approval
- `BLOCKED` (0) - User is blocked and cannot access the system
