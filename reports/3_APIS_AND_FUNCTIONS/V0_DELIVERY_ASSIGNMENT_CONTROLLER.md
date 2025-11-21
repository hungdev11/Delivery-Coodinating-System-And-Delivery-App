# API Documentation: Delivery Assignment Controller (v0)

**Base Path:** `/api/v0/assignments`

This controller acts as a proxy to the `session-service` for listing delivery assignments. All routes require authentication.

| Method | Path | Business Function                             | Java Method Name  | Roles Allowed      | Proxied To                              |
|--------|------|-----------------------------------------------|-------------------|--------------------|-----------------------------------------|
| `POST` | `/`  | List delivery assignments based on a query. | `listAssignments` | Authenticated User | `session-service` (`/api/v0/assignments`) |
