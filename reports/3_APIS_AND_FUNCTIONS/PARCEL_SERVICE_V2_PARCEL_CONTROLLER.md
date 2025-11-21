# Parcel Service API: Parcel Controller (v2)

**Base Path:** `/api/v2/parcels`

This is the controller within the `parcel-service` that handles parcel-related requests for API version v2.

| Method | Path | Business Function                                | Java Method Name | Notes                                                                    |
|--------|------|--------------------------------------------------|------------------|--------------------------------------------------------------------------|
| `POST` | `/`  | Get parcels with enhanced filtering and sorting. | `getParcels`     | This is the backend implementation for the corresponding endpoint in the API gateway. |
