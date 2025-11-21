# API Documentation: Delivery Assignment Controller (v1)

**Base Path:** `/api/v1/assignments`

This controller handles delivery assignments and task status updates. It acts as a proxy to the `session-service`.

| Method | Path                                                  | Business Function                               | Java Method Name          | Proxied To        |
|--------|-------------------------------------------------------|-------------------------------------------------|---------------------------|-------------------|
| `GET`  | `/session/delivery-man/{deliveryManId}/tasks/today`   | Get today's tasks for a delivery man.           | `getDailyTasks`           | `session-service` |
| `GET`  | `/session/{sessionId}/tasks`                          | Get tasks by session ID.                        | `getTasksBySessionId`     | `session-service` |
| `GET`  | `/session/delivery-man/{deliveryManId}/tasks`         | Get task history for a delivery man with filters. | `getTasksHistory`         | `session-service` |
| `POST` | `/drivers/{deliveryManId}/parcels/{parcelId}/complete`  | Mark a task as complete.                        | `completeTask`            | `session-service` |
| `POST` | `/drivers/{deliveryManId}/parcels/{parcelId}/fail`      | Mark a task as failed.                          | `failTask`                | `session-service` |
| `POST` | `/drivers/{deliveryManId}/parcels/{parcelId}/refuse`    | Mark a task as refused.                         | `refuseTask`              | `session-service` |
| `POST` | `/drivers/{deliveryManId}/parcels/{parcelId}/postpone`  | Postpone a task.                                | `postponeTask`            | `session-service` |
| `GET`  | `/current-shipper/parcels/{parcelId}`                 | Get the latest shipper assigned to a parcel.    | `lastestShipperForParcel` | `session-service` |
