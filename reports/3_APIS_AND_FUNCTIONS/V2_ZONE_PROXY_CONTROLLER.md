# API Documentation: Zone Proxy Controller (v2)

**Base Path:** `/api/v2/zones`

This controller acts as a proxy to the `zone-service` for listing zones with enhanced filtering. All routes require authentication.

| Method | Path | Business Function                                 | Java Method Name | Roles Allowed      | Proxied To                       |
|--------|------|---------------------------------------------------|------------------|--------------------|----------------------------------|
| `POST` | `/`  | List zones based on a query (enhanced filtering). | `listZones`      | Authenticated User | `zone-service` (`/api/v2/zones`) |
