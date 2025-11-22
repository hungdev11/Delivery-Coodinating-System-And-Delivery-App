**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# Zone Service API: Address Router (v1)

**Base Path:** `/api/v1/addresses`

This router, part of the `zone-service`, handles a wide range of address and geospatial query operations.

| Method   | Path                    | Business Function                                          | Controller Method      |
|----------|-------------------------|------------------------------------------------------------|------------------------|
| `POST`   | `/`                     | Create a new address.                                      | `createAddress`        |
| `POST`   | `/get-or-create`        | Get an existing address by coordinates or create a new one. | `getOrCreateAddress`   |
| `POST`   | `/batch`                | Batch import addresses.                                    | `batchImport`          |
| `GET`    | `/by-point`             | Find addresses near a specific point.                      | `findByPoint`          |
| `GET`    | `/search`               | Search for addresses by text.                              | `searchByText`         |
| `GET`    | `/nearest`              | Find the nearest addresses to a point.                     | `findNearestAddresses` |
| `GET`    | `/segments/{segmentId}` | Get addresses on a specific road segment.                  | `getAddressesBySegment`|
| `GET`    | `/zones/{zoneId}`       | Get addresses in a specific zone.                          | `getAddressesByZone`   |
| `GET`    | `/{id}`                 | Get an address by its ID.                                  | `getAddress`           |
| `GET`    | `/`                     | List addresses with pagination and filters.                | `listAddresses`        |
| `PUT`    | `/{id}`                 | Update an address.                                         | `updateAddress`        |
| `DELETE` | `/{id}`                 | Delete an address.                                         | `deleteAddress`        |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)