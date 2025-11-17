# API Documentation: Parcel Service Controller (v0)

**Base Path:** `/api/v0/parcels`

This controller acts as a proxy to the `parcel-service` for listing parcels with simple paging.

| Method | Path | Business Function                                | Java Method Name | Roles Allowed | Proxied To                         |
|--------|------|--------------------------------------------------|------------------|---------------|------------------------------------|
| `POST` | `/`  | List parcels based on a query (simple paging). | `listParcels`    | Public        | `parcel-service` (`/api/v0/parcels`) |
