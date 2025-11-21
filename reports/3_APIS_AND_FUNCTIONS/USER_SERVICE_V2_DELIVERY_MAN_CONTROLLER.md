# User Service API: Delivery Man Controller (v2)

**Base Path:** `/api/v2/users/shippers`

This is the controller within the `user-service` that handles delivery man (shipper) related requests for API version v2.

| Method | Path | Business Function                                    | Java Method Name  | Notes                                                                    |
|--------|------|------------------------------------------------------|-------------------|--------------------------------------------------------------------------|
| `POST` | `/`  | Get delivery men (shippers) with enhanced filtering. | `getDeliveryMans` | This is the backend implementation for the corresponding endpoint in the API gateway. |
