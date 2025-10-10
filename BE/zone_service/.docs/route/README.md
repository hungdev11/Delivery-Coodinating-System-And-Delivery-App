# Route Documentation (TOC)

Base URL: `http://localhost:21503/api/v1`

## Table of Contents

- [Health Routes](./health.md)
- [Center Routes](./centers.md)
- [Zone Routes](./zones.md)

## Summary Table

| Prefix           | Module  | Description                     | Doc Link         |
|------------------|---------|---------------------------------|------------------|
| `/health/*`      | health  | Service health and readiness    | [health.md](./health.md) |
| `/centers/*`     | center  | Manage distribution centers     | [centers.md](./centers.md) |
| `/zones/*`       | zone    | Manage delivery zones           | [zones.md](./zones.md) |

## Notes
- All responses follow `BaseResponse<T>`.
- Paginated endpoints return `BaseResponse<PagedData<T>>`.
- See global RESTful standards at `../../RESTFUL.md`.
