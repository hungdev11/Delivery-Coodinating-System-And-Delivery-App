# User Routes (API Gateway - Proxy)

Base URL: `http://localhost:<port>/api/v1/users`

**Note:** These endpoints proxy requests to the User Service. See [User Service Routes](../../User_service/.docs/route/users.md) for detailed documentation.

## Endpoints

All endpoints require authentication (`Authorization: Bearer <token>`).

### GET /users/me
- Description: Get current authenticated user.
- Headers: `Authorization: Bearer <token>`
- Response: Same as User Service GET /users/:id

### POST /users
- Description: Get all users with filtering (requires ADMIN or MANAGER role).
- Body: Same as User Service POST /users
- Response: Same as User Service POST /users

### GET /users/:id
- Description: Get user by ID.
- Response: Same as User Service GET /users/:id

### GET /users/username/:username
- Description: Get user by username.
- Response: Same as User Service GET /users/username/:username

### POST /users/create
- Description: Create user (requires ADMIN role).
- Body: Same as User Service POST /users/create
- Response: Same as User Service POST /users/create

### PUT /users/:id
- Description: Update user.
- Body: Same as User Service PUT /users/:id
- Response: Same as User Service PUT /users/:id

### DELETE /users/:id
- Description: Delete user (requires ADMIN role).
- Response: Same as User Service DELETE /users/:id
