**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# API Documentation: User Controller (v1)

**Base Path:** `/api/v1/users`

This controller manages user operations. All routes require authentication.

| Method   | Path                   | Business Function             | Java Method Name      | Roles Allowed      |
|----------|------------------------|-------------------------------|-----------------------|--------------------|
| `GET`    | `/me`                  | Get current user's info       | `getCurrentUser`      | Authenticated User |
| `POST`   | `/`                    | Get a paginated list of users | `getUsers`            | `ADMIN`, `MANAGER` |
| `GET`    | `/{id}`                | Get user by ID                | `getUserById`         | Authenticated User |
| `GET`    | `/username/{username}` | Get user by username          | `getUserByUsername`   | Authenticated User |
| `POST`   | `/create`              | Create a new user             | `createUser`          | `ADMIN`            |
| `PUT`    | `/{id}`                | Update a user's info          | `updateUser`          | Authenticated User |
| `DELETE` | `/{id}`                | Delete a user                 | `deleteUser`          | `ADMIN`            |

*Note: "Authenticated User" means any user who is logged in.*


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)