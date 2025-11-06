# Parcel Routes (API Gateway - Proxy)

## API Versions

- **V0**: Simple paging and sorting - `http://localhost:<port>/api/v0/parcels`
- **V1**: Dynamic filtering (group-level) - `http://localhost:<port>/api/v1/parcels`
- **V2**: Enhanced filtering (pair-level) - `http://localhost:<port>/api/v2/parcels`

**Note:** These endpoints proxy requests to the Parcel Service. See [Parcel Service Routes](../../parcel-service/.docs/route/parcels.md) for detailed documentation.

## Endpoints

All endpoints follow the same structure as Parcel Service routes:

### V0 Endpoints
- `POST /v0/parcels` - List parcels with simple paging (no dynamic filters)

### V1 Endpoints
- `POST /v1/parcels` - List parcels with filtering (group-level operations)

### V2 Endpoints
- `POST /v2/parcels` - List parcels with enhanced filtering (pair-level operations)

### Common Endpoints (All Versions)
- `POST /parcels` - Create parcel
- `GET /parcels/:parcelId` - Get parcel by ID
- `GET /parcels/code/:code` - Get parcel by code
- `GET /parcels/me` - Get parcels sent by customer
- `GET /parcels/me/receive` - Get parcels received by customer
- `PUT /parcels/:parcelId` - Update parcel
- `DELETE /parcels/:parcelId` - Delete parcel
- `PUT /parcels/change-status/:parcelId` - Change parcel status

For request/response formats, see [Parcel Service documentation](../../parcel-service/.docs/route/parcels.md).
