# API Documentation: User Shipper Controller (v2)

**Base Path:** `/api/v2/users/shippers`

This controller acts as a proxy to the `user-service` for listing shippers (delivery men) with advanced filtering. All routes require authentication.

| Method | Path | Business Function                                    | Java Method Name | Roles Allowed      | Proxied To                                  |
|--------|------|------------------------------------------------------|------------------|--------------------|---------------------------------------------|
| `POST` | `/`  | List shippers (delivery men) with advanced filtering. | `listShippers`   | Authenticated User | `user-service` (`/api/v2/users/shippers`) |
