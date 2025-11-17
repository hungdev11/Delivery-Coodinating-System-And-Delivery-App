# User Service API: User Controller (v0)

**Base Path:** `/api/v0/users`

This is the controller within the `user-service` that handles user-related requests for API version v0.

| Method | Path | Business Function                           | Java Method Name | Notes                                                                    |
|--------|------|---------------------------------------------|------------------|--------------------------------------------------------------------------|
| `POST` | `/`  | Get users with simple paging and sorting. | `getUsers`       | This is the backend implementation for the corresponding endpoint in the API gateway. |
