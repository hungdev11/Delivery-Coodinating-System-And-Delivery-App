**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# API Documentation: Delivery Session Controller (v1)

**Base Path:** `/api/v1/delivery-sessions`

This controller handles complex, cross-service operations related to delivery sessions, orchestrated by a service layer within the API Gateway. All routes require authentication.

| Method | Path                                          | Business Function                                                       | Java Method Name                  | Notes                                            |
|--------|-----------------------------------------------|-------------------------------------------------------------------------|-----------------------------------|--------------------------------------------------|
| `GET`  | `/{sessionId}/with-assignments`               | Get a delivery session and all its associated delivery assignments.     | `getSessionWithAssignments`       | Orchestrates calls to multiple services.         |
| `GET`  | `/{sessionId}/demo-route`                     | Calculate and return a demo route for all assignments in a session.     | `getDemoRouteForSession`          | Involves route calculation logic within the gateway. |
| `PUT`  | `/{sessionId}/assignments/{assignmentId}/status` | Update the status of a delivery assignment and its corresponding parcel. | `updateAssignmentAndParcelStatus` | Orchestrates calls to multiple services.         |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)