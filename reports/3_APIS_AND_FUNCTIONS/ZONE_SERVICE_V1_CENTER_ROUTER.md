# Zone Service API: Center Router (v1)

**Base Path:** `/api/v1/centers`

This router, part of the `zone-service`, handles CRUD operations for delivery centers.

| Method   | Path         | Business Function         | Controller Method |
|----------|--------------|---------------------------|-------------------|
| `GET`    | `/`          | Get all centers.          | `getAllCenters`   |
| `GET`    | `/{id}`      | Get a center by its ID.   | `getCenterById`   |
| `GET`    | `/code/{code}` | Get a center by its code. | `getCenterByCode` |
| `POST`   | `/`          | Create a new center.      | `createCenter`    |
| `PUT`    | `/{id}`      | Update a center.          | `updateCenter`    |
| `DELETE` | `/{id}`      | Delete a center.          | `deleteCenter`    |
