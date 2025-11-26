**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# Zone Service API: Zone Router (v1)

**Base Path:** `/api/v1/zones`

This router, part of the `zone-service`, handles CRUD operations for delivery zones.

| Method   | Path                 | Business Function                                  | Controller Method     |
|----------|----------------------|----------------------------------------------------|-----------------------|
| `POST`   | `/`                  | Get zones with filtering and sorting.              | `getZones`            |
| `GET`    | `/{id}`              | Get a zone by its ID.                              | `getZoneById`         |
| `POST`   | `/create`            | Create a new zone.                                 | `createZone`          |
| `PUT`    | `/{id}`              | Update a zone.                                     | `updateZone`          |
| `DELETE` | `/{id}`              | Delete a zone.                                     | `deleteZone`          |
| `GET`    | `/filterable-fields` | Get fields that can be used for filtering zones.   | `getFilterableFields` |
| `GET`    | `/sortable-fields`   | Get fields that can be used for sorting zones.     | `getSortableFields`   |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)