# Route Documentation (TOC)

Base URL: `http://localhost:8080/api/v1`

## Table of Contents

- [Health Routes](./health.md)
- [Authentication Routes](./auth.md)
- [User Routes](./users.md)
- [Settings Proxy Routes](./settings.md)
- [Zone Proxy Routes](./zones.md)

## Summary Table

| Prefix           | Module       | Description                            | Doc Link         |
|------------------|--------------|----------------------------------------|------------------|
| `/health/*`      | health       | Service health check                   | [health.md](./health.md) |
| `/auth/*`        | auth         | Keycloak authentication & user sync    | [auth.md](./auth.md) |
| `/users/*`       | users        | User management (proxied)              | [users.md](./users.md) |
| `/settings/*`    | settings     | Settings management (proxied)          | [settings.md](./settings.md) |
| `/zones/*`       | zones        | Zone & delivery management (proxied)   | [zones.md](./zones.md) |

## Notes
- All responses follow `BaseResponse<T>`.
- Most endpoints require JWT authentication via Keycloak.
- Public endpoints are marked with `@PublicRoute` annotation.
- Proxied endpoints forward requests to their respective microservices.
- See global RESTful standards at `../../../RESTFUL.md`.
