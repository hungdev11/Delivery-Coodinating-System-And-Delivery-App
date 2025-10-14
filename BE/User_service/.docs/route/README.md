# Route Documentation (TOC)

Base URL: `http://localhost:8081/api/v1`

## Table of Contents

- [User Routes](./users.md)

## Summary Table

| Prefix           | Module  | Description                     | Doc Link         |
|------------------|---------|---------------------------------|------------------|
| `/users/*`       | user    | User management and sync        | [users.md](./users.md) |

## Notes
- All responses follow `BaseResponse<T>`.
- User service integrates with Keycloak for authentication.
- See global RESTful standards at `../../../RESTFUL.md`.
