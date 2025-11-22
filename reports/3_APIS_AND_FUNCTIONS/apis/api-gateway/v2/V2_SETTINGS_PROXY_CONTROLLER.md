**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# API Documentation: Settings Proxy Controller (v2)

**Base Path:** `/api/v2/settings`

This controller acts as a proxy to the `settings-service` for listing settings with enhanced filtering.

| Method | Path | Business Function                                    | Java Method Name | Roles Allowed | Proxied To                             |
|--------|------|------------------------------------------------------|------------------|---------------|----------------------------------------|
| `POST` | `/`  | List settings based on a query (enhanced filtering). | `listSettings`   | Public        | `settings-service` (`/api/v2/settings`) |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)