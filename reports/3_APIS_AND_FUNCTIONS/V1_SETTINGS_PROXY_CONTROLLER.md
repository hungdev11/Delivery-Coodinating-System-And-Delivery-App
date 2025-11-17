# API Documentation: Settings Proxy Controller (v1)

**Base Path:** `/api/v1/settings`

This controller acts as a proxy to the `settings-service` for managing application settings.

| Method   | Path                  | Business Function                             | Java Method Name     | Roles Allowed      | Proxied To         |
|----------|-----------------------|-----------------------------------------------|----------------------|--------------------|--------------------|
| `POST`   | `/`                   | List settings based on a query.               | `listSettings`       | Public             | `settings-service` |
| `GET`    | `/{group}`            | Get all settings for a specific group (service). | `getSettingsByGroup` | Public             | `settings-service` |
| `GET`    | `/{group}/{key}`      | Get a specific setting by its group and key.  | `getSetting`         | Public             | `settings-service` |
| `GET`    | `/{group}/{key}/value`| Get only the value of a specific setting.     | `getSettingValue`    | Public             | `settings-service` |
| `PUT`    | `/{group}/{key}`      | Create or update (upsert) a setting.          | `upsertSetting`      | Authenticated User | `settings-service` |
| `DELETE` | `/{group}/{key}`      | Delete a setting.                             | `deleteSetting`      | Authenticated User | `settings-service` |
