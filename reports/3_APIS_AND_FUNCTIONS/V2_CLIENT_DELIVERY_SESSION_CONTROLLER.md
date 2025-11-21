# API Documentation: Client Delivery Session Controller (v2)

**Base Path:** `/api/v2/client/delivery-sessions`

This controller acts as a proxy to the `session-service` for listing delivery sessions for a client. All routes require authentication.

| Method | Path | Business Function                    | Java Method Name       | Roles Allowed      | Proxied To                                    |
|--------|------|--------------------------------------|------------------------|--------------------|-----------------------------------------------|
| `POST` | `/`  | List delivery sessions for a client. | `listDeliverySessions` | Authenticated User | `session-service` (`/api/v2/delivery-sessions`) |
