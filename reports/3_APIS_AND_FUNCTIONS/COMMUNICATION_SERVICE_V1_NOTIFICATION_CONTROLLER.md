# Communication Service API: Notification Controller (v1)

**Base Path:** `/api/v1/notifications`

This controller, part of the `communication-service`, handles notification management for users.

| Method   | Path                    | Business Function                             | Java Method Name         | Notes                       |
|----------|-------------------------|-----------------------------------------------|--------------------------|-----------------------------|
| `GET`    | `/`                     | Get notifications for the current user (paginated). | `getNotifications`       | Requires `X-User-Id` header.|
| `GET`    | `/unread`               | Get unread notifications for the current user.  | `getUnreadNotifications` | Requires `X-User-Id` header.|
| `GET`    | `/unread/count`         | Get the count of unread notifications.        | `getUnreadCount`         | Requires `X-User-Id` header.|
| `PUT`    | `/{notificationId}/read`| Mark a notification as read.                  | `markAsRead`             | Requires `X-User-Id` header.|
| `PUT`    | `/read-all`             | Mark all notifications as read.               | `markAllAsRead`          | Requires `X-User-Id` header.|
| `DELETE` | `/{notificationId}`     | Delete a notification.                        | `deleteNotification`     | Requires `X-User-Id` header.|
