# Session Service API: Delivery Session Controller (v2)

**Base Path:** `/api/v2/delivery-sessions`

This is the controller within the `session-service` that handles delivery session requests for API version v2.

| Method | Path | Business Function                                        | Java Method Name | Notes                                                                    |
|--------|------|----------------------------------------------------------|------------------|--------------------------------------------------------------------------|
| `POST` | `/`  | Search delivery sessions with enhanced filtering and sorting. | `searchSessions` | This is the backend implementation for the corresponding endpoint in the API gateway. |
