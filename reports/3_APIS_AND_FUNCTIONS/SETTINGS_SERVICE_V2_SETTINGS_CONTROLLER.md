# Settings Service API: Settings Controller (v2)

**Base Path:** `/api/v2/settings`

This is the controller within the `Settings_service` that handles settings-related requests for API version v2.

| Method | Path | Business Function                               | Java Method Name | Notes                                                                    |
|--------|------|-------------------------------------------------|------------------|--------------------------------------------------------------------------|
| `POST` | `/`  | Get settings with enhanced filtering and sorting. | `getSettings`    | This is the backend implementation for the corresponding endpoint in the API gateway. |
