# Zone Routes (API Gateway - Proxy)

Base URL: `http://localhost:<port>/api/v1`

**Note:** These endpoints proxy requests to the Zone Service. See [Zone Service Routes](../../zone_service/.docs/route/zones.md) for detailed documentation.

## Endpoints

All endpoints follow the same structure as Zone Service routes:

- `POST /zones` - List zones with filtering
- `GET /zones/:id` - Get zone by ID
- `GET /zones/code/:code` - Get zone by code
- `GET /zones/center/:centerId` - Get zones by center
- `POST /zones/create` - Create zone
- `PUT /zones/:id` - Update zone
- `DELETE /zones/:id` - Delete zone
- `GET /zones/filterable-fields` - Get filterable fields
- `GET /zones/sortable-fields` - Get sortable fields

For request/response formats, see [Zone Service documentation](../../zone_service/.docs/route/zones.md).
