# Parcel Routes (API Gateway - Proxy)

Base URL: `http://localhost:<port>/api/v1/parcels`

**Note:** These endpoints proxy requests to the Parcel Service. See [Parcel Service Routes](../../parcel-service/.docs/route/parcels.md) for detailed documentation.

## Endpoints

All endpoints follow the same structure as Parcel Service routes:

- `POST /parcels` - Create parcel
- `GET /parcels/:parcelId` - Get parcel by ID
- `GET /parcels/code/:code` - Get parcel by code
- `GET /parcels` - List parcels with filtering
- `GET /parcels/me` - Get parcels sent by customer
- `GET /parcels/me/receive` - Get parcels received by customer
- `PUT /parcels/:parcelId` - Update parcel
- `DELETE /parcels/:parcelId` - Delete parcel
- `PUT /parcels/change-status/:parcelId` - Change parcel status

For request/response formats, see [Parcel Service documentation](../../parcel-service/.docs/route/parcels.md).
