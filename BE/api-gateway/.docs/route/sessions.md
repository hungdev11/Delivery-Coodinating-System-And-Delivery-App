# Session Routes (API Gateway - Proxy)

Base URL: `http://localhost:<port>/api/v1/sessions`

**Note:** These endpoints proxy requests to the Session Service. See [Session Service Routes](../../session-service/.docs/route/sessions.md) for detailed documentation.

## Endpoints

- `POST /sessions/drivers/:deliveryManId/accept-parcel` - Accept parcel to session
- `GET /sessions/:sessionId` - Get session details
- `POST /sessions/:sessionId/complete` - Complete session
- `POST /sessions/:sessionId/fail` - Fail session
- `POST /sessions` - Create batch session

For request/response formats, see [Session Service documentation](../../session-service/.docs/route/sessions.md).
