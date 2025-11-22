**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# User Service API: User Address Controller (v1)

**Base Path:** `/api/v1/users`

This controller is the backend implementation for managing user addresses, corresponding to the `UserAddressProxyController` in the API Gateway.

## Current User Endpoints

These endpoints operate on the currently authenticated user, identified by the `X-User-Id` header.

**Base Path:** `/api/v1/users/me/addresses`

| Method   | Path                  | Business Function                              | Java Method Name      |
|----------|-----------------------|------------------------------------------------|-----------------------|
| `POST`   | `/`                   | Create an address for the current user.        | `createMyAddress`     |
| `GET`    | `/`                   | Get all addresses for the current user.        | `getMyAddresses`      |
| `GET`    | `/primary`            | Get the primary address for the current user.  | `getMyPrimaryAddress` |
| `GET`    | `/{addressId}`        | Get a specific address for the current user.   | `getMyAddress`        |
| `PUT`    | `/{addressId}`        | Update an address for the current user.        | `updateMyAddress`     |
| `DELETE` | `/{addressId}`        | Delete an address for the current user.        | `deleteMyAddress`     |
| `PUT`    | `/{addressId}/set-primary` | Set an address as primary for the current user. | `setMyPrimaryAddress` |

## Admin Endpoints

These endpoints are for administrators to manage the addresses of any user.

**Base Path:** `/api/v1/users/{userId}/addresses`

| Method   | Path           | Business Function                           | Java Method Name        |
|----------|----------------|---------------------------------------------|-------------------------|
| `POST`   | `/`            | Create an address for a specific user.      | `createUserAddress`     |
| `GET`    | `/`            | Get all addresses for a specific user.      | `getUserAddresses`      |
| `GET`    | `/primary`     | Get the primary address for a specific user. | `getUserPrimaryAddress` |
| `GET`    | `/{addressId}` | Get a specific address for a specific user. | `getUserAddress`        |
| `PUT`    | `/{addressId}` | Update an address for a specific user.      | `updateUserAddress`     |
| `DELETE` | `/{addressId}` | Delete an address for a specific user.      | `deleteUserAddress`     |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)