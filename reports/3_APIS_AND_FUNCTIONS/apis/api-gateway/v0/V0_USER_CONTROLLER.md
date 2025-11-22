**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# API Documentation: User Controller (v0)

**Base Path:** `/api/v0/users`

This controller acts as a proxy to the `user-service` for listing users with simple paging. All routes require authentication.

| Method | Path | Business Function                                | Java Method Name | Roles Allowed      | Proxied To                     |
|--------|------|--------------------------------------------------|------------------|--------------------|--------------------------------|
| `POST` | `/`  | List users based on a query (simple paging). | `listUsers`      | Authenticated User | `user-service` (`/api/v0/users`) |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)