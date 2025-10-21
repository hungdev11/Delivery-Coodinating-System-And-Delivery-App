# Users Module

This module handles user management functionality including CRUD operations.

## Features

- List all users with pagination
- Search users by username, email, or name
- Create new users
- Edit existing users
- Delete users
- Bulk delete multiple users
- Export users to CSV
- View user details

## File Structure

```
Users/
├── api.ts                # API client functions
├── model.type.ts         # TypeScript types and classes
├── UsersView.vue         # Main view component
├── Users.routes.ts       # Route definitions
└── README.md             # This file
```

## Types

### UserDto
Represents a user entity with the following properties:
- `id`: Unique identifier (UUID)
- `keycloakId`: Keycloak user ID (optional)
- `username`: User's username
- `email`: User's email address
- `firstName`: First name
- `lastName`: Last name
- `phone`: Phone number (optional)
- `address`: Address (optional)
- `identityNumber`: Identity/ID number (optional)
- `status`: User status (ACTIVE, INACTIVE, SUSPENDED, PENDING)
- `createdAt`: Creation timestamp
- `updatedAt`: Last update timestamp

Methods:
- `fullName`: Returns concatenated first and last name
- `displayStatus`: Returns human-readable status

### CreateUserRequest
Request payload for creating a new user:
- `username`: Required
- `email`: Required
- `firstName`: Required
- `lastName`: Required
- `phone`: Optional
- `address`: Optional
- `identityNumber`: Optional
- `password`: Optional

### UpdateUserRequest
Request payload for updating a user (all fields optional):
- `email`
- `firstName`
- `lastName`
- `phone`
- `address`
- `identityNumber`
- `status`

## API Functions

### getUsers(params?)
Get paginated list of users.

**Parameters:**
- `params.page`: Page number (0-indexed)
- `params.size`: Page size
- `params.search`: Search query

**Returns:** `Promise<GetUsersResponse>`

### getCurrentUser()
Get the current authenticated user.

**Returns:** `Promise<GetUserResponse>`

### getUserById(id)
Get user by ID.

**Parameters:**
- `id`: User UUID

**Returns:** `Promise<GetUserResponse>`

### getUserByUsername(username)
Get user by username.

**Parameters:**
- `username`: Username string

**Returns:** `Promise<GetUserResponse>`

### createUser(data)
Create a new user.

**Parameters:**
- `data`: CreateUserRequest object

**Returns:** `Promise<CreateUserResponse>`

### updateUser(id, data)
Update an existing user.

**Parameters:**
- `id`: User UUID
- `data`: UpdateUserRequest object

**Returns:** `Promise<UpdateUserResponse>`

### deleteUser(id)
Delete a user.

**Parameters:**
- `id`: User UUID

**Returns:** `Promise<DeleteUserResponse>`

## Components

### UsersView
Main view component that provides:
- Data table with sortable columns
- Search functionality
- Pagination
- Bulk selection and actions
- Create user modal
- Edit user modal
- Delete confirmation dialog
- Export to CSV

## Usage

### Add to Router

```typescript
import { usersRoutes } from '@/modules/Users/Users.routes'

const router = createRouter({
  routes: [
    ...usersRoutes,
    // other routes
  ],
})
```

### Navigation Link

```vue
<RouterLink to="/users">Manage Users</RouterLink>
```

### Bulk Actions

The Users module supports bulk operations:
- **Bulk Delete**: Select multiple users and delete them
- **Bulk Export**: Export selected users to CSV

Selections persist across pages, allowing you to select items from multiple pages before performing an action.

## Permissions

Access to the Users module requires authentication and one of the following roles:
- ADMIN
- MANAGER

## Dependencies

- Vue 3
- Vue Router
- Pinia (state management)
- Nuxt UI (component library)
- Axios (HTTP client)

## Backend API

The module communicates with the following backend endpoints:

- `GET /v1/users` - List users (paginated)
- `GET /v1/users/me` - Get current user
- `GET /v1/users/:id` - Get user by ID
- `GET /v1/users/username/:username` - Get user by username
- `POST /v1/users` - Create user
- `PUT /v1/users/:id` - Update user
- `DELETE /v1/users/:id` - Delete user

All endpoints require authentication via JWT token in the Authorization header.

## Error Handling

All API errors are handled by the global axios interceptor and displayed via toast notifications.

## Future Enhancements

- User profile image upload
- Advanced filters (by role, status, date range)
- User activity logs
- Password reset functionality
- Email verification
- Import users from CSV
- User groups/teams management
