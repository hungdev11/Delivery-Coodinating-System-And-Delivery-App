**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# API Documentation: Delivery Session Controller (v2)

**Base Path:** `/api/v2/delivery-sessions`

This controller acts as a proxy to the `session-service` for listing delivery sessions. All routes require authentication.

| Method | Path | Business Function                          | Java Method Name       | Roles Allowed      | Proxied To                                    |
|--------|------|--------------------------------------------|------------------------|--------------------|-----------------------------------------------|
| `POST` | `/`  | List delivery sessions based on a query. | `listDeliverySessions` | Authenticated User | `session-service` (`/api/v2/delivery-sessions`) |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)