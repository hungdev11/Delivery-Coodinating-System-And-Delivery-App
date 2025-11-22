**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# API Documentation: Session Controller (v1)

**Base Path:** `/api/v1/sessions`

This controller acts as a proxy to the `session-service` for all session-related operations.

| Method | Path                                     | Business Function                                                    | Java Method Name        | Proxied To        |
|--------|------------------------------------------|----------------------------------------------------------------------|-------------------------|-------------------|
| `POST` | `/drivers/{deliveryManId}/accept-parcel` | Accept a parcel into a delivery session.                             | `acceptParcelToSession` | `session-service` |
| `GET`  | `/{sessionId}`                           | Get session details by ID.                                           | `getSessionById`        | `session-service` |
| `POST` | `/{sessionId}/complete`                  | Mark a session as complete.                                          | `completeSession`       | `session-service` |
| `POST` | `/{sessionId}/fail`                      | Mark a session as failed.                                            | `failSession`           | `session-service` |
| `POST` | `/`                                      | Create a batch of sessions.                                          | `createSessionBatch`    | `session-service` |
| `POST` | `/drivers/{deliveryManId}/prepare`       | Create a session in 'CREATED' state (shipper starts a session).      | `createSessionPrepared` | `session-service` |
| `POST` | `/{sessionId}/start`                     | Start a session (move from 'CREATED' to 'IN_PROGRESS').              | `startSession`          | `session-service` |
| `GET`  | `/drivers/{deliveryManId}/active`        | Get the active session ('CREATED' or 'IN_PROGRESS') for a shipper. | `getActiveSession`      | `session-service` |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)