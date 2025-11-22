**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# API Documentation: Client Delivery Session Controller (v1)

**Base Path:** `/api/v1/client/delivery-sessions`

This controller handles client-specific operations for delivery sessions. It acts as a proxy to the `session-service`.

| Method | Path | Business Function                      | Java Method Name         | Roles Allowed      | Proxied To                                    |
|--------|------|----------------------------------------|--------------------------|--------------------|-----------------------------------------------|
| `POST` | `/`  | Search delivery sessions for client users. | `searchDeliverySessions` | Authenticated User | `session-service` (`/api/v2/delivery-sessions`) |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)