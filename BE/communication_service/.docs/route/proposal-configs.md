# Proposal Configuration Routes (Admin)

Base URL: `http://localhost:<port>/api/v1/admin/proposals/configs`

## Endpoints

### GET /admin/proposals/configs
- Description: Get all proposal configurations.
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

### POST /admin/proposals/configs
- Description: Create a new proposal configuration.
- Body:
```json
{
  "proposalType": "CANCEL_DELIVERY",
  "requiredRole": "SHIPPER",
  "title": "Cancel Delivery",
  "description": "Request to cancel a delivery",
  "isActive": true
}
```
- Response 201:
```json
{
  "id": "uuid",
  "proposalType": "CANCEL_DELIVERY",
  "requiredRole": "SHIPPER",
  "title": "Cancel Delivery",
  "description": "Request to cancel a delivery",
  "isActive": true
}
```

### PUT /admin/proposals/configs/:configId
- Description: Update a proposal configuration.
- Params: `configId`
- Body:
```json
{
  "proposalType": "CANCEL_DELIVERY",
  "requiredRole": "SHIPPER",
  "title": "Cancel Delivery (Updated)",
  "description": "Updated description",
  "isActive": false
}
```
- Response 200:
```json
{
  "id": "uuid",
  "proposalType": "CANCEL_DELIVERY",
  "requiredRole": "SHIPPER",
  "title": "Cancel Delivery (Updated)",
  "description": "Updated description",
  "isActive": false
}
```

### DELETE /admin/proposals/configs/:configId
- Description: Delete a proposal configuration.
- Params: `configId`
- Response 204: No Content
