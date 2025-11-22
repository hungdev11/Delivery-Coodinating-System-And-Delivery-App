**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# Settings Service API: Settings Controller (v1)

**Base Path:** `/api/v1/settings`

This controller provides the core CRUD and search functionality for system settings.

| Method   | Path                   | Business Function                                     | Java Method Name     |
|----------|------------------------|-------------------------------------------------------|----------------------|
| `POST`   | `/`                    | List settings with filtering, sorting, and paging.    | `getSettings`        |
| `GET`    | `/{group}`             | Get all settings for a specific group.                | `getSettingsByGroup` |
| `GET`    | `/{group}/{key}`       | Get a specific setting by its group and key.          | `getSetting`         |
| `GET`    | `/{group}/{key}/value` | Get only the value of a specific setting.             | `getSettingValue`    |
| `PUT`    | `/{group}/{key}`       | Create or update (upsert) a setting.                  | `upsertSetting`      |
| `DELETE` | `/{group}/{key}`       | Delete a setting.                                     | `deleteSetting`      |
| `PUT`    | `/{group}/bulk`        | Bulk create or update multiple settings in a group. | `bulkUpsertSettings` |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)