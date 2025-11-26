**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# API Documentation: Settings Proxy Controller (v0)

**Base Path:** `/api/v0/settings`

This controller acts as a proxy to the `settings-service` for listing settings with simple paging.

| Method | Path | Business Function                                 | Java Method Name | Roles Allowed | Proxied To                             |
|--------|------|---------------------------------------------------|------------------|---------------|----------------------------------------|
| `POST` | `/`  | List settings based on a query (simple paging). | `listSettings`   | Public        | `settings-service` (`/api/v0/settings`) |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)