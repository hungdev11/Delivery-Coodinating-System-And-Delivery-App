**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# Session Service API: Delivery Assignment Controller (v1)

**Base Path:** `/api/v1/assignments`

This controller manages the actions on individual delivery assignments (tasks) within the `session-service`.

| Method | Path                                                  | Business Function                                         | Java Method Name                 | Notes        |
|--------|-------------------------------------------------------|-----------------------------------------------------------|----------------------------------|--------------|
| `GET`  | `/session/delivery-man/{deliveryManId}/tasks/today`   | Get tasks for the currently active session of a shipper.  | `getDailyTasks`                  | Deprecated.  |
| `GET`  | `/session/{sessionId}/tasks`                          | Get all tasks for a specific session.                     | `getTasksBySessionId`            |              |
| `GET`  | `/session/delivery-man/{deliveryManId}/tasks`         | Get task history for a shipper with filters.              | `getTasksHistory`                |              |
| `POST` | `/drivers/{deliveryManId}/parcels/{parcelId}/complete`  | Mark a task as successfully delivered.                    | `completeTask`                   |              |
| `POST` | `/drivers/{deliveryManId}/parcels/{parcelId}/fail`      | Mark a task as failed.                                    | `failTask`                       |              |
| `POST` | `/drivers/{deliveryManId}/parcels/{parcelId}/refuse`    | Mark a task as refused by the customer.                   | `refuseTask`                     |              |
| `POST` | `/drivers/{deliveryManId}/parcels/{parcelId}/postpone`  | Mark a task as postponed by the customer.                 | `postponeTask`                   |              |
| `GET`  | `/current-shipper/parcels/{parcelId}`                 | Get info about the latest shipper assigned to a parcel. | `getCurrentShipperInfoForParcel` |              |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)