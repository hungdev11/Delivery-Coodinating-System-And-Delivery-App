# API Documentation: Auth Controller (v1)

**Base Path:** `/api/v1/auth`

This controller handles Keycloak authentication requests, including login, token validation, and user synchronization.

| Method | Path              | Business Function                                                    | Java Method Name  | Public/Authenticated |
|--------|-------------------|----------------------------------------------------------------------|-------------------|----------------------|
| `POST` | `/login`          | Login with username/password for a specific client type (backend/frontend). | `login`           | Public               |
| `POST` | `/login/default`  | Login with default realm and client.                                 | `defaultLogin`    | Public               |
| `POST` | `/login/custom`   | Login with a specific realm and client ID.                           | `customLogin`     | Public               |
| `POST` | `/validate-token` | Validate a JWT token and return user info.                           | `validateToken`   | Public               |
| `POST` | `/refresh-token`  | Refresh an access token using a refresh token.                       | `refreshToken`    | Public               |
| `POST` | `/logout`         | Logout user by invalidating the refresh token.                       | `logout`          | Public               |
| `GET`  | `/me`             | Get current user info from the JWT token.                            | `getCurrentUser`  | Authenticated        |
| `POST` | `/sync`           | Sync user data from Keycloak to the local User Service DB after login. | `syncUser`        | Authenticated        |
