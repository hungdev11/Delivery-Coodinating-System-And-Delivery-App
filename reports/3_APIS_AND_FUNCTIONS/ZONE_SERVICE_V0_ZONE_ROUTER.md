# Zone Service API: Zone Router (v0)

**Base Path:** `/api/v0/zones`

This router, part of the `zone-service`, handles zone requests for API version v0.

| Method | Path | Business Function                           | Controller Method |
|--------|------|---------------------------------------------|-------------------|
| `POST` | `/`  | Get zones with simple paging and sorting. | `getZones`        |
