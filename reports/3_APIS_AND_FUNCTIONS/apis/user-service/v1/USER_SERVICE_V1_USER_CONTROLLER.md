**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# User Service API: User Controller (v1)

**Base Path:** `/api/v1/users`

This controller provides the core CRUD and search functionality for users within the `user-service`.

| Method   | Path                   | Business Function                                            | Java Method Name      | Notes                                           |
|----------|------------------------|--------------------------------------------------------------|-----------------------|-------------------------------------------------|
| `POST`   | `/create`              | Create a new user.                                           | `createUser`          | Uses Keycloak ID as the primary key.            |
| `GET`    | `/{id}`                | Get user by ID.                                              | `getUser`             |                                                 |
| `GET`    | `/me`                  | Get the current user's info.                                 | `getCurrentUser`      | Currently not implemented.                      |
| `GET`    | `/username/{username}` | Get user by username.                                        | `getUserByUsername`   |                                                 |
| `POST`   | `/`                    | Get all users with advanced filtering and sorting.           | `getUsers`            | Enriches response with user roles from Keycloak.|
| `PUT`    | `/{id}`                | Update a user.                                               | `updateUser`          |                                                 |
| `DELETE` | `/{id}`                | Delete a user.                                               | `deleteUser`          |                                                 |
| `POST`   | `/sync`                | Synchronize user data from Keycloak to the local database. | `upsertByKeycloakId`  |                                                 |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)