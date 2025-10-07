# Route Documentation (TOC)

Base URL: `http://localhost:8082/api/v1`

## Table of Contents

- [Health Routes](./health.md)
- [Settings Routes](./settings.md)

## Summary Table

| Prefix           | Module   | Description                     | Doc Link         |
|------------------|----------|---------------------------------|------------------|
| `/health`        | health   | Health check endpoints          | [health.md](./health.md) |
| `/settings/*`    | settings | System settings management      | [settings.md](./settings.md) |

## Notes
- All responses follow `BaseResponse<T>`.
- Settings can be organized by groups and filtered by levels.
- See global RESTful standards at `../../../RESTFUL.md`.
