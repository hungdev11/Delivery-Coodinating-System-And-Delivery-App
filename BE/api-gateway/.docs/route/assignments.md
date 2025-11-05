# Assignment Routes (API Gateway - Proxy)

Base URL: `http://localhost:<port>/api/v1/assignments`

**Note:** These endpoints proxy requests to the Session Service. See [Assignment Service Routes](../../session-service/.docs/route/assignments.md) for detailed documentation.

## Endpoints

- `GET /assignments/session/delivery-man/:deliveryManId/tasks/today` - Get daily tasks
- `GET /assignments/session/delivery-man/:deliveryManId/tasks` - Get task history
- `POST /assignments/drivers/:deliveryManId/parcels/:parcelId/complete` - Complete task
- `POST /assignments/drivers/:deliveryManId/parcels/:parcelId/fail` - Fail task
- `POST /assignments/drivers/:deliveryManId/parcels/:parcelId/refuse` - Refuse task
- `POST /assignments/drivers/:deliveryManId/parcels/:parcelId/postpone` - Postpone task
- `GET /assignments/current-shipper/parcels/:parcelId` - Get current shipper

For request/response formats, see [Assignment Service documentation](../../session-service/.docs/route/assignments.md).
