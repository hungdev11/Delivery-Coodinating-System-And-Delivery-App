# Session Service API: Delivery Assignment Controller (v2)

**Base Path:** `/api/v2/assignments`

This is the controller within the `session-service` that handles delivery assignment requests for API version v2.

| Method | Path | Business Function                                      | Java Method Name | Notes                                                                    |
|--------|------|--------------------------------------------------------|------------------|--------------------------------------------------------------------------|
| `POST` | `/`  | Get delivery assignments with enhanced filtering and sorting. | `getAssignments` | This is the backend implementation for the corresponding endpoint in the API gateway. |
