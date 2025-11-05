# User Routes

Base URL: `http://localhost:<port>/api/v1/users`

## Endpoints

### POST /users
- Description: Get all users with advanced filtering and sorting (paginated).
- Body:
```json
{
  "page": 0,
  "size": 10,
  "search": "optional",
  "filters": [],
  "sorts": [],
  "selected": []
}
```
- Response 200:
```json
{
  "status": "success",
  "data": {
    "data": [{
      "id": "uuid",
      "keycloakId": "keycloak-uuid",
      "username": "testuser",
      "email": "test@example.com",
      "firstName": "Test",
      "lastName": "User",
      "phone": "1234567890",
      "address": "123 Main St",
      "identityNumber": "123456789",
      "status": "ACTIVE"
    }],
    "page": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
  }
}
```

### POST /users/create
- Description: Create a new user.
- Body:
```json
{
  "keycloakId": "keycloak-uuid",
  "username": "testuser",
  "email": "test@example.com",
  "firstName": "Test",
  "lastName": "User",
  "phone": "1234567890",
  "address": "123 Main St",
  "identityNumber": "123456789",
  "status": "ACTIVE"
}
```
- Response 201:
```json
{
  "status": "success",
  "message": "User created successfully",
  "data": {
    "id": "uuid",
    "username": "testuser",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "phone": "1234567890",
    "address": "123 Main St",
    "status": "ACTIVE"
  }
}
```

### GET /users/:id
- Description: Get user by ID.
- Response 200:
```json
{
  "status": "success",
  "data": {
    "id": "uuid",
    "username": "testuser",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "phone": "1234567890",
    "address": "123 Main St",
    "status": "ACTIVE"
  }
}
```
- Response 404:
```json
{
  "status": "error",
  "message": "User not found"
}
```

### GET /users/username/:username
- Description: Get user by username.
- Response 200: Same as GET /users/:id
- Response 404:
```json
{
  "status": "error",
  "message": "User not found"
}
```

### GET /users/me
- Description: Get current authenticated user.
- Response 501:
```json
{
  "status": "error",
  "message": "Current user endpoint not implemented"
}
```

### PUT /users/:id
- Description: Update user.
- Body (all fields optional):
```json
{
  "email": "newemail@example.com",
  "firstName": "New",
  "lastName": "Name",
  "phone": "9876543210",
  "address": "456 New St",
  "identityNumber": "987654321",
  "status": "INACTIVE"
}
```
- Response 200:
```json
{
  "status": "success",
  "message": "User updated successfully",
  "data": {
    "id": "uuid",
    "username": "testuser",
    "email": "newemail@example.com",
    "firstName": "New",
    "lastName": "Name",
    "status": "INACTIVE"
  }
}
```

### DELETE /users/:id
- Description: Delete user.
- Response 200:
```json
{
  "status": "success",
  "message": "User deleted successfully"
}
```
- Response 404:
```json
{
  "status": "error",
  "message": "User not found"
}
```

### POST /users/sync
- Description: Sync user from Keycloak (create or update).
- Body:
```json
{
  "keycloakId": "keycloak-uuid",
  "username": "testuser",
  "email": "test@example.com",
  "firstName": "Test",
  "lastName": "User"
}
```
- Response 200:
```json
{
  "status": "success",
  "message": "User synced successfully",
  "data": {
    "id": "uuid",
    "keycloakId": "keycloak-uuid",
    "username": "testuser",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User"
  }
}
```
