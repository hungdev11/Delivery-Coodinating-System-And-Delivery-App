# Zone Routes (API Gateway - Proxy)

## API Versions

- **V0**: Simple paging and sorting - `http://localhost:<port>/api/v0/zones`
- **V1**: Dynamic filtering (group-level) - `http://localhost:<port>/api/v1/zones`
- **V2**: Enhanced filtering (pair-level) - `http://localhost:<port>/api/v2/zones`

**Note:** These endpoints proxy requests to the Zone Service. See [Zone Service Routes](../../zone_service/.docs/route/zones.md) for detailed documentation.

## Endpoints

All endpoints follow the same structure as Zone Service routes:

### V0 Endpoints
- `POST /v0/zones` - List zones with simple paging (no dynamic filters)

### V1 Endpoints
- `POST /v1/zones` - List zones with filtering (group-level operations)

### V2 Endpoints
- `POST /v2/zones` - List zones with enhanced filtering (pair-level operations)

### Common Endpoints (All Versions)
- `GET /zones/:id` - Get zone by ID
- `GET /zones/code/:code` - Get zone by code
- `GET /zones/center/:centerId` - Get zones by center
- `POST /zones/create` - Create zone
- `PUT /zones/:id` - Update zone
- `DELETE /zones/:id` - Delete zone
- `GET /zones/filterable-fields` - Get filterable fields
- `GET /zones/sortable-fields` - Get sortable fields

For request/response formats, see [Zone Service documentation](../../zone_service/.docs/route/zones.md).
