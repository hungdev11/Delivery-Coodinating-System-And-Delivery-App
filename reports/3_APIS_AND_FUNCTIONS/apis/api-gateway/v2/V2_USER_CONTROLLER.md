**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# API Documentation: User Controller (v2)

**Base Path:** `/api/v2/users`

This controller acts as a proxy to the `user-service` for listing users with enhanced filtering. All routes require authentication.

| Method | Path | Business Function                                 | Java Method Name | Roles Allowed      | Proxied To                     |
|--------|------|---------------------------------------------------|------------------|--------------------|--------------------------------|
| `POST` | `/`  | List users based on a query (enhanced filtering). | `listUsers`      | Authenticated User | `user-service` (`/api/v2/users`) |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)