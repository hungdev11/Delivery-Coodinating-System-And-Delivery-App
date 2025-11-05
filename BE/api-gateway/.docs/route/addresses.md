# Address Routes (API Gateway - Proxy)

Base URL: `http://localhost:<port>/api/v1`

**Note:** These endpoints proxy requests to the Zone Service. See [Address Service Routes](../../zone_service/.docs/route/addresses.md) for detailed documentation.

## Endpoints

All endpoints follow the same structure as Zone Service routes:

- `GET /addresses` - List addresses
- `GET /addresses/:id` - Get address by ID
- `POST /addresses` - Create address
- `PUT /addresses/:id` - Update address
- `DELETE /addresses/:id` - Delete address
- `POST /addresses/batch` - Batch import addresses
- `GET /addresses/nearest` - Find nearest addresses
- `GET /addresses/by-point` - Find addresses by point
- `GET /addresses/search` - Search addresses
- `GET /addresses/segments/:segmentId` - Get addresses by segment
- `GET /addresses/zones/:zoneId` - Get addresses by zone

For request/response formats, see [Address Service documentation](../../zone_service/.docs/route/addresses.md).
