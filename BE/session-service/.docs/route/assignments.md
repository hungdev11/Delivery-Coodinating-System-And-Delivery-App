# Delivery Assignment Routes

Base URL: `http://localhost:<port>/api/v1/assignments`

## Endpoints

### GET /assignments/session/delivery-man/:deliveryManId/tasks/today
- Description: Get daily tasks (from active session) for a delivery man.
- Params: `deliveryManId`
- Query: `status` (optional, array), `page` (default: 0), `size` (default: 10)
- Response 200:
```json
{
  "content": [
    {
      "assignmentId": "uuid",
      "parcelId": "parcel-uuid",
      "sessionId": "uuid",
      "deliveryManId": "user-uuid",
      "status": "ACCEPTED",
      "assignedAt": "2025-01-15T10:30:00Z"
    }
  ],
  "page": {
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### GET /assignments/session/delivery-man/:deliveryManId/tasks
- Description: Get task history for a delivery man (from completed sessions).
- Params: `deliveryManId`
- Query: `status` (optional, array), `createdAtStart`, `createdAtEnd`, `completedAtStart`, `completedAtEnd`, `page` (default: 0), `size` (default: 10)
- Response 200:
```json
{
  "content": [
    {
      "assignmentId": "uuid",
      "parcelId": "parcel-uuid",
      "sessionId": "uuid",
      "deliveryManId": "user-uuid",
      "status": "COMPLETED",
      "assignedAt": "2025-01-15T10:30:00Z",
      "completedAt": "2025-01-15T14:30:00Z"
    }
  ],
  "page": {
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### POST /assignments/drivers/:deliveryManId/parcels/:parcelId/complete
- Description: Complete a task (delivery successful).
- Params: `deliveryManId`, `parcelId`
- Body:
```json
{
  "distance": 5000.5,
  "duration": 600,
  "geometry": {
    "type": "LineString",
    "coordinates": [[106.7718, 10.8505], [106.8032, 10.8623]]
  }
}
```
- Response 200:
```json
{
  "assignmentId": "uuid",
  "parcelId": "parcel-uuid",
  "status": "COMPLETED",
  "completedAt": "2025-01-15T14:30:00Z"
}
```

### POST /assignments/drivers/:deliveryManId/parcels/:parcelId/fail
- Description: Fail a task (delivery failed).
- Params: `deliveryManId`, `parcelId`
- Body:
```json
{
  "reason": "Customer not available",
  "routeInfo": {
    "distance": 5000.5,
    "duration": 600,
    "geometry": {
      "type": "LineString",
      "coordinates": [[106.7718, 10.8505], [106.8032, 10.8623]]
    }
  }
}
```
- Response 200:
```json
{
  "assignmentId": "uuid",
  "parcelId": "parcel-uuid",
  "status": "FAILED",
  "failureReason": "Customer not available",
  "failedAt": "2025-01-15T14:30:00Z"
}
```

### POST /assignments/drivers/:deliveryManId/parcels/:parcelId/refuse
- Description: Refuse a task (customer rejected).
- Params: `deliveryManId`, `parcelId`
- Response 200:
```json
{
  "assignmentId": "uuid",
  "parcelId": "parcel-uuid",
  "status": "REJECTED",
  "rejectedAt": "2025-01-15T14:30:00Z"
}
```

### POST /assignments/drivers/:deliveryManId/parcels/:parcelId/postpone
- Description: Postpone a task (customer requested delay).
- Params: `deliveryManId`, `parcelId`
- Body:
```json
"2025-01-16T10:00:00Z"
```
- Response 200:
```json
{
  "assignmentId": "uuid",
  "parcelId": "parcel-uuid",
  "status": "POSTPONED",
  "postponedAt": "2025-01-15T14:30:00Z",
  "postponeReason": "Khách yêu cầu hoãn với thời gian: 2025-01-16T10:00:00Z"
}
```

### GET /assignments/current-shipper/parcels/:parcelId
- Description: Get current shipper info for a parcel.
- Params: `parcelId`
- Response 200:
```json
{
  "shipperId": "user-uuid",
  "shipperName": "Shipper Name",
  "assignmentId": "uuid",
  "status": "ACCEPTED"
}
```
- Response 200 (null): If no shipper assigned
