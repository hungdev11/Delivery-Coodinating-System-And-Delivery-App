# Settings Service API: Settings Controller (v0)

**Base Path:** `/api/v0/settings`

This is the controller within the `Settings_service` that handles settings-related requests for API version v0.

| Method | Path | Business Function                            | Java Method Name | Notes                                                                    |
|--------|------|----------------------------------------------|------------------|--------------------------------------------------------------------------|
| `POST` | `/`  | Get settings with simple paging and sorting. | `getSettings`    | This is the backend implementation for the corresponding endpoint in the API gateway. |
