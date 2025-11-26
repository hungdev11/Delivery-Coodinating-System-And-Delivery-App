**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# API Documentation: User Address Proxy Controller (v1)

**Base Path:** `/api/v1/users`

This controller acts as a proxy to the `user-service` for managing user addresses. All routes require authentication.

## Current User Endpoints

These endpoints operate on the currently authenticated user.

**Base Path:** `/api/v1/users/me/addresses`

| Method   | Path                  | Business Function                              | Java Method Name      | Proxied To       |
|----------|-----------------------|------------------------------------------------|-----------------------|------------------|
| `POST`   | `/`                   | Create an address for the current user.        | `createMyAddress`     | `user-service`   |
| `GET`    | `/`                   | Get all addresses for the current user.        | `getMyAddresses`      | `user-service`   |
| `GET`    | `/primary`            | Get the primary address for the current user.  | `getMyPrimaryAddress` | `user-service`   |
| `GET`    | `/{addressId}`        | Get a specific address for the current user.   | `getMyAddress`        | `user-service`   |
| `PUT`    | `/{addressId}`        | Update an address for the current user.        | `updateMyAddress`     | `user-service`   |
| `DELETE` | `/{addressId}`        | Delete an address for the current user.        | `deleteMyAddress`     | `user-service`   |
| `PUT`    | `/{addressId}/set-primary` | Set an address as primary for the current user. | `setMyPrimaryAddress` | `user-service`   |

## Admin Endpoints

These endpoints are for administrators to manage the addresses of any user.

**Base Path:** `/api/v1/users/{userId}/addresses`

| Method   | Path           | Business Function                           | Java Method Name        | Proxied To       |
|----------|----------------|---------------------------------------------|-------------------------|------------------|
| `POST`   | `/`            | Create an address for a specific user.      | `createUserAddress`     | `user-service`   |
| `GET`    | `/`            | Get all addresses for a specific user.      | `getUserAddresses`      | `user-service`   |
| `GET`    | `/primary`     | Get the primary address for a specific user. | `getUserPrimaryAddress` | `user-service`   |
| `GET`    | `/{addressId}` | Get a specific address for a specific user. | `getUserAddress`        | `user-service`   |
| `PUT`    | `/{addressId}` | Update an address for a specific user.      | `updateUserAddress`     | `user-service`   |
| `DELETE` | `/{addressId}` | Delete an address for a specific user.      | `deleteUserAddress`     | `user-service`   |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)