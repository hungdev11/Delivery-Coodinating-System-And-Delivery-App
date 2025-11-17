# Session Service API: Delivery Assignment Controller (v0)

**Base Path:** `/api/v0/assignments`

This is the controller within the `session-service` that handles delivery assignment requests for API version v0.

| Method | Path | Business Function                                     | Java Method Name | Notes                                                                    |
|--------|------|-------------------------------------------------------|------------------|--------------------------------------------------------------------------|
| `POST` | `/`  | Get delivery assignments with simple paging and sorting. | `getAssignments` | This is the backend implementation for the corresponding endpoint in the API gateway. |
