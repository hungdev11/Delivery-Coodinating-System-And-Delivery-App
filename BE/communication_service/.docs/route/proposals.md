# Proposal Routes

Base URL: `http://localhost:<port>/api/v1/proposals`

## Endpoints

### POST /proposals
- Description: Create a new proposal (e.g., shipper cancellation request).
- Body:
```json
{
  "proposalType": "CANCEL_DELIVERY",
  "targetId": "parcel-uuid",
  "initiatorId": "user-uuid",
  "recipientId": "user-uuid",
  "data": {
    "reason": "Vehicle breakdown"
  }
}
```
- Response 200:
```json
{
  "proposalId": "uuid",
  "proposalType": "CANCEL_DELIVERY",
  "status": "PENDING",
  "initiatorId": "user-uuid",
  "recipientId": "user-uuid",
  "targetId": "parcel-uuid",
  "createdAt": "2025-01-15T10:30:00Z"
}
```

### POST /proposals/:proposalId/respond
- Description: Respond to a proposal.
- Params: `proposalId`
- Query: `userId` (required)
- Body:
```json
{
  "resultData": "{\"approved\": true, \"comment\": \"Approved\"}"
}
```
- Response 200:
```json
{
  "proposalId": "uuid",
  "proposalType": "CANCEL_DELIVERY",
  "status": "ACCEPTED",
  "initiatorId": "user-uuid",
  "recipientId": "user-uuid",
  "targetId": "parcel-uuid",
  "resultData": "{\"approved\": true}",
  "updatedAt": "2025-01-15T10:35:00Z"
}
```

### GET /proposals/available-configs
- Description: Get available proposal configurations for specific roles.
- Query: `roles` (array of role names, e.g., `roles=SHIPPER&roles=ADMIN`)
- Response 200:
```json
[
  {
    "id": "uuid",
    "proposalType": "CANCEL_DELIVERY",
    "requiredRole": "SHIPPER",
    "title": "Cancel Delivery",
    "description": "Request to cancel a delivery",
    "isActive": true
  }
]
```
