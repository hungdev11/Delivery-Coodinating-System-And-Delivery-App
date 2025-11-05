# OSRM Routes (API Gateway - Proxy)

Base URL: `http://localhost:<port>/api/v1`

**Note:** These endpoints proxy requests to the Zone Service. See [OSRM Service Routes](../../zone_service/.docs/route/osrm.md) for detailed documentation.

## Endpoints

All OSRM management endpoints are proxied:

- `POST /osrm/build/:instanceId` - Build OSRM instance
- `POST /osrm/build-all` - Build all instances
- `POST /osrm/start/:instanceId` - Start instance
- `POST /osrm/stop/:instanceId` - Stop instance
- `POST /osrm/rolling-restart` - Rolling restart
- `GET /osrm/status` - Get all instances status
- `GET /osrm/status/:instanceId` - Get instance status
- `GET /osrm/health` - Health check
- `GET /osrm/validate/:instanceId` - Validate data

For request/response formats, see [OSRM Service documentation](../../zone_service/.docs/route/osrm.md).
