# Session Routes

Base URL: `http://localhost:<port>/api/v1/sessions`

## Endpoints

### POST /sessions/drivers/:deliveryManId/accept-parcel
- Description: Accept a parcel to session (find-or-create session).
- Params: `deliveryManId`
- Body:
```json
{
  "parcelId": "parcel-uuid"
}
```
- Response 201:
```json
{
  "assignmentId": "uuid",
  "parcelId": "parcel-uuid",
  "sessionId": "uuid",
  "deliveryManId": "user-uuid",
  "status": "ACCEPTED",
  "assignedAt": "2025-01-15T10:30:00Z"
}
```

### GET /sessions/:sessionId
- Description: Get session details with all tasks.
- Params: `sessionId`
- Response 200:
```json
{
  "sessionId": "uuid",
  "deliveryManId": "user-uuid",
  "status": "ACTIVE",
  "startedAt": "2025-01-15T10:00:00Z",
  "tasks": [
    {
      "assignmentId": "uuid",
      "parcelId": "parcel-uuid",
      "status": "ACCEPTED"
    }
  ]
}
```

### POST /sessions/:sessionId/complete
- Description: Complete a session.
- Params: `sessionId`
- Response 200:
```json
{
  "sessionId": "uuid",
  "deliveryManId": "user-uuid",
  "status": "COMPLETED",
  "startedAt": "2025-01-15T10:00:00Z",
  "completedAt": "2025-01-15T18:00:00Z"
}
```

### POST /sessions/:sessionId/fail
- Description: Fail a session (e.g., vehicle breakdown, accident).
- Params: `sessionId`
- Body:
```json
{
  "reason": "Vehicle breakdown"
}
```
- Response 200:
```json
{
  "sessionId": "uuid",
  "deliveryManId": "user-uuid",
  "status": "FAILED",
  "startedAt": "2025-01-15T10:00:00Z",
  "failedAt": "2025-01-15T15:00:00Z",
  "failureReason": "Vehicle breakdown"
}
```

### POST /sessions
- Description: Create a batch session with multiple parcels.
- Body:
```json
{
  "deliveryManId": "user-uuid",
  "parcelIds": ["parcel-uuid-1", "parcel-uuid-2"]
}
```
- Response 201:
```json
{
  "sessionId": "uuid",
  "deliveryManId": "user-uuid",
  "status": "ACTIVE",
  "startedAt": "2025-01-15T10:00:00Z",
  "tasks": [
    {
      "assignmentId": "uuid-1",
      "parcelId": "parcel-uuid-1",
      "status": "ACCEPTED"
    },
    {
      "assignmentId": "uuid-2",
      "parcelId": "parcel-uuid-2",
      "status": "ACCEPTED"
    }
  ]
}
```
