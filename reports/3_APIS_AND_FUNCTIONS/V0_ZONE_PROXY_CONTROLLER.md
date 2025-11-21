# API Documentation: Zone Proxy Controller (v0)

**Base Path:** `/api/v0/zones`

This controller acts as a proxy to the `zone-service` for listing zones with simple paging. All routes require authentication.

| Method | Path | Business Function                                | Java Method Name | Roles Allowed      | Proxied To                       |
|--------|------|--------------------------------------------------|------------------|--------------------|----------------------------------|
| `POST` | `/`  | List zones based on a query (simple paging). | `listZones`      | Authenticated User | `zone-service` (`/api/v0/zones`) |
