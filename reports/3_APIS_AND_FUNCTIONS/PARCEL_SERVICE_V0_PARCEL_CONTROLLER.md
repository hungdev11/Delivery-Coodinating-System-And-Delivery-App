# Parcel Service API: Parcel Controller (v0)

**Base Path:** `/api/v0/parcels`

This is the controller within the `parcel-service` that handles parcel-related requests for API version v0.

| Method | Path | Business Function                           | Java Method Name | Notes                                                                    |
|--------|------|---------------------------------------------|------------------|--------------------------------------------------------------------------|
| `POST` | `/`  | Get parcels with simple paging and sorting. | `getParcels`     | This is the backend implementation for the corresponding endpoint in the API gateway. |
