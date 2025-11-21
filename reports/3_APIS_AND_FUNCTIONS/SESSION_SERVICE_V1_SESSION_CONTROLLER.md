# Session Service API: Session Controller (v1)

**Base Path:** `/api/v1/sessions`

This controller manages the lifecycle of delivery sessions within the `session-service`.

| Method | Path                                          | Business Function                                            | Java Method Name         | Notes                                         |
|--------|-----------------------------------------------|--------------------------------------------------------------|--------------------------|-----------------------------------------------|
| `POST` | `/drivers/{deliveryManId}/accept-parcel`      | Accept a parcel into a delivery session (find-or-create).    | `acceptParcelToSession`  | Backend for gateway.                          |
| `GET`  | `/{sessionId}`                                | Get session details by ID, including all tasks.              | `getSessionById`         | Backend for gateway.                          |
| `POST` | `/{sessionId}/complete`                       | Shipper completes the session.                               | `completeSession`        | Backend for gateway.                          |
| `POST` | `/{sessionId}/fail`                           | Shipper reports an incident and fails the session.           | `failSession`            | Backend for gateway.                          |
| `POST` | `/`                                           | Create a session with a batch of parcels.                    | `createSessionBatch`     | Backend for gateway.                          |
| `POST` | `/drivers/{deliveryManId}/prepare`            | Create a session in 'CREATED' state.                         | `createSessionPrepared`  | Backend for gateway.                          |
| `POST` | `/{sessionId}/start`                          | Start a session ('CREATED' -> 'IN_PROGRESS').                | `startSession`           | Backend for gateway.                          |
| `GET`  | `/drivers/{deliveryManId}/active`             | Get the active session for a shipper.                        | `getActiveSession`       | Backend for gateway.                          |
| `PUT`  | `/{sessionId}/assignments/{assignmentId}/status` | Update assignment status within a session.                   | `updateAssignmentStatus` | Used by the API gateway for nested queries.   |
