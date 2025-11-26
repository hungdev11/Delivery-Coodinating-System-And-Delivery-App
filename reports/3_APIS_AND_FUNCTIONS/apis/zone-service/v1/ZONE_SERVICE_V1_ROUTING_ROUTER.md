**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# Zone Service API: Routing Router (v1)

**Base Path:** `/api/v1/routing`

This router, part of the `zone-service`, handles route calculations and OSRM instance management.

| Method | Path               | Business Function                                         | Controller Method        |
|--------|--------------------|-----------------------------------------------------------|--------------------------|
| `POST` | `/route`           | Calculate a route between multiple waypoints.             | `calculateRoute`         |
| `POST` | `/priority-route`  | Calculate a priority-based multi-stop route for delivery. | `calculatePriorityRoute` |
| `GET`  | `/simple`          | Get a route between two points using query parameters.    | `getSimpleRoute`         |
| `GET`  | `/status`          | Get the status of the OSRM instances.                     | `getOSRMStatus`          |
| `POST` | `/switch-instance` | Switch the active OSRM instance (admin endpoint).         | `switchOSRMInstance`     |
| `POST` | `/demo-route`      | Calculate a demo route with priority-based ordering.      | `calculateDemoRoute`     |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)