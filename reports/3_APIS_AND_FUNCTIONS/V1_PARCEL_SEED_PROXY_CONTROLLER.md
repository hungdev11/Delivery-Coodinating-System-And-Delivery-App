# API Documentation: Parcel Seed Proxy Controller (v1)

**Base Path:** `/api/v1/parcels/seed`

This controller acts as a proxy for seeding parcels, forwarding the request to the `user-service`.

| Method | Path | Business Function                                   | Java Method Name | Proxied To                              |
|--------|------|-----------------------------------------------------|------------------|-----------------------------------------|
| `POST` | `/`  | Seed parcels, potentially for testing or demo purposes. | `seedParcels`    | `user-service` (`/api/v1/parcels/seed`) |
