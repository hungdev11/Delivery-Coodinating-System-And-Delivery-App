# Settings Routes (API Gateway - Proxy)

Base URL: `http://localhost:<port>/api/v1/settings`

**Note:** These endpoints proxy requests to the Settings Service. See [Settings Service Routes](../../Settings_service/.docs/route/settings.md) for detailed documentation.

## Endpoints

- `POST /settings` - List settings with filtering
- `GET /settings/:group` - Get settings by group
- `GET /settings/:group/:key` - Get setting by group/key
- `GET /settings/:group/:key/value` - Get setting value
- `PUT /settings/:group/:key` - Upsert setting
- `DELETE /settings/:group/:key` - Delete setting

For request/response formats, see [Settings Service documentation](../../Settings_service/.docs/route/settings.md).
