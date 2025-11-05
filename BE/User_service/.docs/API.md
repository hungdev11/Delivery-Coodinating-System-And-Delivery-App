# User Service API Reference

This document provides detailed information about the User Service's RESTful API.

## Base URL

`http://<host>:<port>/api/v1/users`

## Common Response Object

All API responses are wrapped in a `BaseResponse<T>` object:

```json
{
  "status": "success" | "error",
  "message": "A descriptive message",
  "data": { ... } // The actual response data (T)
}
```

---

## Endpoints

### 1. Get Users (with filtering)

Returns a paginated list of users based on complex filter, sort, and pagination criteria.

*   **URL**: `/`
*   **Method**: `POST`
*   **Request Body**: `PagingRequest` object. See [Dynamic Query System](./DYNAMIC_QUERY.md) for details.
*   **Success Response**: `200 OK`
    ```json
    {
      "status": "success",
      "data": {
        "data": [ ...userDto ],
        "page": {
          "page": 0,
          "size": 10,
          "totalElements": 100,
          "totalPages": 10
        }
      }
    }
    ```

### 2. Create User

Creates a new user in the system.

*   **URL**: `/create`
*   **Method**: `POST`
*   **Request Body**:
    ```json
    {
      "keycloakId": "...",
      "username": "testuser",
      "email": "test@example.com",
      "firstName": "Test",
      "lastName": "User",
      "phone": "1234567890",
      "address": "123 Main St",
      "identityNumber": "...",
      "status": "ACTIVE"
    }
    ```
*   **Success Response**: `201 CREATED`
    ```json
    {
      "status": "success",
      "message": "User created successfully",
      "data": { ...userDto }
    }
    ```

### 3. Get User by ID

Retrieves a single user by their UUID.

*   **URL**: `/{id}`
*   **Method**: `GET`
*   **URL Params**: `id=[string]` (User's UUID)
*   **Success Response**: `200 OK`
*   **Error Response**: `404 NOT FOUND` if user does not exist.

### 4. Get User by Username

Retrieves a single user by their username.

*   **URL**: `/username/{username}`
*   **Method**: `GET`
*   **URL Params**: `username=[string]`
*   **Success Response**: `200 OK`
*   **Error Response**: `404 NOT FOUND` if user does not exist.

### 5. Get Current User

Retrieves the currently authenticated user (based on JWT token).

*   **URL**: `/me`
*   **Method**: `GET`
*   **Note**: This endpoint may not be fully implemented in development environments.

### 6. Update User

Updates an existing user's information.

*   **URL**: `/{id}`
*   **Method**: `PUT`
*   **URL Params**: `id=[string]` (User's UUID)
*   **Request Body**: `UpdateUserRequest` object (similar to create, but without username/keycloakId).
*   **Success Response**: `200 OK`
    ```json
    {
      "status": "success",
      "message": "User updated successfully",
      "data": { ...userDto }
    }
    ```

### 7. Delete User

Deletes a user from the system.

*   **URL**: `/{id}`
*   **Method**: `DELETE`
*   **URL Params**: `id=[string]` (User's UUID)
*   **Success Response**: `200 OK`
    ```json
    {
      "status": "success",
      "message": "User deleted successfully"
    }
    ```

### 8. Sync User from Keycloak

Creates a new user or updates an existing one based on their Keycloak ID. This is used to keep the user database in sync with the identity provider.

*   **URL**: `/sync`
*   **Method**: `POST`
*   **Request Body**:
    ```json
    {
      "keycloakId": "...",
      "username": "testuser",
      "email": "test@example.com",
      "firstName": "Test",
      "lastName": "User"
    }
    ```
*   **Success Response**: `200 OK`
    ```json
    {
      "status": "success",
      "message": "User synced successfully",
      "data": { ...userDto }
    }
    ```
