# Center Routes (API Gateway - Proxy)

Base URL: `http://localhost:<port>/api/v1`

**Note:** These endpoints proxy requests to the Zone Service. See [Center Service Routes](../../zone_service/.docs/route/centers.md) for detailed documentation.

## Endpoints

All endpoints follow the same structure as Zone Service routes:

- `GET /centers` - List centers
- `GET /centers/:id` - Get center by ID
- `GET /centers/code/:code` - Get center by code
- `POST /centers` - Create center
- `PUT /centers/:id` - Update center
- `DELETE /centers/:id` - Delete center

For request/response formats, see [Center Service documentation](../../zone_service/.docs/route/centers.md).
