# User Service API: Parcel Seed Controller (v1)

**Base Path:** `/api/v1/parcels/seed`

This controller is responsible for seeding parcel data for testing and demonstration purposes. It is the backing implementation for the corresponding endpoint in the API Gateway.

| Method | Path | Business Function                                                              | Java Method Name |
|--------|------|--------------------------------------------------------------------------------|------------------|
| `POST` | `/`  | Create parcels randomly or with a specific shop/client for testing/demo purposes. | `seedParcels`    |
