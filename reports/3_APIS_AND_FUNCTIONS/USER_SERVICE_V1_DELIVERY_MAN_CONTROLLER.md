# User Service API: Delivery Man Controller (v1)

**Base Path:** `/api/v1/users`

This is the controller within the `user-service` that handles delivery man-related requests for API version v1.

| Method | Path                     | Business Function                      | Java Method Name         |
|--------|--------------------------|----------------------------------------|--------------------------|
| `GET`  | `/{userId}/delivery-man` | Get delivery man details by user ID. | `getDeliveryManByUserId` |
