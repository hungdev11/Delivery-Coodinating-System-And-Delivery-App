# User Routes (API Gateway - Proxy)

## API Versions

- **V0**: Simple paging and sorting (no dynamic filters) - `http://localhost:<port>/api/v0/users`
- **V1**: Dynamic filtering with group-level operations - `http://localhost:<port>/api/v1/users`
- **V2**: Enhanced filtering with pair-level operations - `http://localhost:<port>/api/v2/users`

**Note:** These endpoints proxy requests to the User Service. See [User Service Routes](../../User_service/.docs/route/users.md) for detailed documentation.

## Endpoints

All endpoints require authentication (`Authorization: Bearer <token>`).

### V0 Endpoints

#### POST /v0/users
- Description: Get all users with simple paging (no dynamic filters).
- Headers: `Authorization: Bearer <token>`
- Body: Same as User Service POST /v0/users
- Response: Same as User Service POST /v0/users

### V1 Endpoints

#### POST /v1/users
- Description: Get all users with filtering (requires ADMIN or MANAGER role).
- Headers: `Authorization: Bearer <token>`
- Body: Same as User Service POST /v1/users
- Response: Same as User Service POST /v1/users

### V2 Endpoints

#### POST /v2/users
- Description: Get all users with enhanced filtering (requires ADMIN or MANAGER role).
- Headers: `Authorization: Bearer <token>`
- Body: Same as User Service POST /v2/users
- Response: Same as User Service POST /v2/users

### Common Endpoints (All Versions)

#### GET /users/me
- Description: Get current authenticated user.
- Headers: `Authorization: Bearer <token>`
- Response: Same as User Service GET /users/:id

#### GET /users/:id
- Description: Get user by ID.
- Headers: `Authorization: Bearer <token>`
- Response: Same as User Service GET /users/:id

#### GET /users/username/:username
- Description: Get user by username.
- Headers: `Authorization: Bearer <token>`
- Response: Same as User Service GET /users/username/:username

#### POST /users/create
- Description: Create user (requires ADMIN role).
- Headers: `Authorization: Bearer <token>`
- Body: Same as User Service POST /users/create
- Response: Same as User Service POST /users/create

#### PUT /users/:id
- Description: Update user.
- Headers: `Authorization: Bearer <token>`
- Body: Same as User Service PUT /users/:id
- Response: Same as User Service PUT /users/:id

#### DELETE /users/:id
- Description: Delete user (requires ADMIN role).
- Headers: `Authorization: Bearer <token>`
- Response: Same as User Service DELETE /users/:id
