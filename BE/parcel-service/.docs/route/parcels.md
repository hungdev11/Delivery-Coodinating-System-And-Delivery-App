# Parcel Routes

## API Versions

- **V0**: Simple paging and sorting (no dynamic filters) - `http://localhost:<port>/api/v0/parcels`
- **V1**: Dynamic filtering with group-level operations - `http://localhost:<port>/api/v1/parcels`
- **V2**: Enhanced filtering with pair-level operations - `http://localhost:<port>/api/v2/parcels`

## V0 Endpoints (Simple Paging)

### POST /v0/parcels
- Description: Get all parcels with simple paging and sorting (no dynamic filters).
- Body:
```json
{
  "page": 0,
  "size": 10,
  "sorts": [
    {
      "field": "createdAt",
      "direction": "desc"
    }
  ],
  "search": "optional",
  "selected": []
}
```
- Response 200:
```json
{
  "result": {
    "data": [{
      "id": "uuid",
      "code": "PARCEL001",
      "status": "PENDING",
      "createdAt": "2025-01-15T10:30:00Z"
    }],
    "page": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1, "filters": null, "sorts": [...] }
  }
}
```

## V1 Endpoints (Dynamic Filtering - Group Level)

### POST /v1/parcels (Replaces GET /v1/parcels)
- Description: Get all parcels with advanced filtering and sorting.
- Body:
```json
{
  "page": 0,
  "size": 10,
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "status",
        "operator": "eq",
        "value": "PENDING"
      }
    ]
  },
  "sorts": [
    {
      "field": "createdAt",
      "direction": "desc"
    }
  ],
  "selected": []
}
```
- Response 200: Same as V0

## V2 Endpoints (Enhanced Filtering - Pair Level)

### POST /v2/parcels
- Description: Get all parcels with enhanced filtering (operations between each pair).
- Body:
```json
{
  "page": 0,
  "size": 10,
  "filters": {
    "type": "group",
    "items": [
      {
        "type": "condition",
        "field": "status",
        "operator": "EQUALS",
        "value": "PENDING"
      },
      {
        "type": "operator",
        "value": "OR"
      },
      {
        "type": "condition",
        "field": "status",
        "operator": "EQUALS",
        "value": "IN_TRANSIT"
      }
    ]
  },
  "sorts": [],
  "selected": []
}
```
- Response 200: Same as V0

## Common Endpoints (All Versions)

### POST /parcels
- Description: Create a new parcel.
- Base URL: `http://localhost:<port>/api/v1/parcels`
- Body:
```json
{
  "code": "PARCEL001",
  "senderId": "user-uuid",
  "receiverId": "user-uuid",
  "senderAddress": "123 Main St",
  "receiverAddress": "456 Oak Ave",
  "senderLat": 10.762622,
  "senderLon": 106.660172,
  "receiverLat": 10.775000,
  "receiverLon": 106.680000,
  "weight": 2.5,
  "value": 1000000,
  "description": "Package description"
}
```
- Response 201:
```json
{
  "id": "uuid",
  "code": "PARCEL001",
  "senderId": "user-uuid",
  "receiverId": "user-uuid",
  "status": "PENDING",
  "createdAt": "2025-01-15T10:30:00Z"
}
```

### GET /parcels/:parcelId
- Description: Get parcel by ID.
- Params: `parcelId`
- Response 200:
```json
{
  "id": "uuid",
  "code": "PARCEL001",
  "senderId": "user-uuid",
  "receiverId": "user-uuid",
  "status": "PENDING",
  "createdAt": "2025-01-15T10:30:00Z"
}
```

### GET /parcels/code/:code
- Description: Get parcel by code.
- Params: `code`
- Response 200: Same as GET /parcels/:parcelId

### GET /parcels
- Description: Get parcels with filtering and pagination.
- Query: `status`, `deliveryType`, `createdAtStart`, `createdAtEnd`, `senderId`, `receiverId`, `page` (default: 0), `size` (default: 10), `sortBy`, `direction` (default: "asc")
- Response 200:
```json
{
  "content": [
    {
      "id": "uuid",
      "code": "PARCEL001",
      "status": "PENDING",
      "createdAt": "2025-01-15T10:30:00Z"
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

### GET /parcels/me
- Description: Get parcels sent by customer.
- Query: `customerId` (required), `page` (default: 0), `size` (default: 10)
- Response 200: Same as GET /parcels

### GET /parcels/me/receive
- Description: Get parcels received by customer.
- Query: `customerId` (required), `page` (default: 0), `size` (default: 10)
- Response 200: Same as GET /parcels

### PUT /parcels/:parcelId
- Description: Update parcel.
- Params: `parcelId`
- Body (all fields optional):
```json
{
  "senderAddress": "789 New St",
  "receiverAddress": "321 Elm St",
  "weight": 3.0,
  "value": 1500000,
  "description": "Updated description"
}
```
- Response 200:
```json
{
  "id": "uuid",
  "code": "PARCEL001",
  "status": "PENDING",
  "weight": 3.0,
  "value": 1500000
}
```

### DELETE /parcels/:parcelId
- Description: Delete parcel.
- Params: `parcelId`
- Response 204: No Content

### PUT /parcels/change-status/:parcelId
- Description: Change parcel status (generic).
- Params: `parcelId`
- Query: `event` (required, enum: DELIVERY_SUCCESSFUL, CUSTOMER_RECEIVED, CAN_NOT_DELIVERY, CUSTOMER_REJECT, POSTPONE, CUSTOMER_CONFIRM_NOT_RECEIVED, MISSUNDERSTANDING_DISPUTE, FAULT_DISPUTE)
- Response 200:
```json
{
  "id": "uuid",
  "code": "PARCEL001",
  "status": "DELIVERED"
}
```

### PUT /parcels/deliver/:parcelId
- Description: Mark parcel as delivered (shipper).
- Params: `parcelId`
- Response 200: Same as PUT /parcels/change-status/:parcelId

### PUT /parcels/confirm/:parcelId
- Description: Confirm parcel received (customer).
- Params: `parcelId`
- Response 200: Same as PUT /parcels/change-status/:parcelId

### PUT /parcels/accident/:parcelId
- Description: Report accident/cannot deliver (shipper).
- Params: `parcelId`
- Response 200: Same as PUT /parcels/change-status/:parcelId

### PUT /parcels/refuse/:parcelId
- Description: Refuse parcel (customer).
- Params: `parcelId`
- Response 200: Same as PUT /parcels/change-status/:parcelId

### PUT /parcels/postpone/:parcelId
- Description: Postpone delivery (shipper).
- Params: `parcelId`
- Response 200: Same as PUT /parcels/change-status/:parcelId

### PUT /parcels/dispute/:parcelId
- Description: Create dispute - customer not received (customer).
- Params: `parcelId`
- Response 200: Same as PUT /parcels/change-status/:parcelId

### PUT /parcels/resolve-dispute/misunderstanding/:parcelId
- Description: Resolve dispute as misunderstanding (admin).
- Params: `parcelId`
- Response 200: Same as PUT /parcels/change-status/:parcelId

### PUT /parcels/resolve-dispute/fault/:parcelId
- Description: Resolve dispute as fault/lost (admin).
- Params: `parcelId`
- Response 200: Same as PUT /parcels/change-status/:parcelId

### POST /parcels/bulk
- Description: Fetch multiple parcels by IDs.
- Body:
```json
["parcel-uuid-1", "parcel-uuid-2"]
```
- Response 200:
```json
{
  "parcel-uuid-1": {
    "id": "parcel-uuid-1",
    "code": "PARCEL001",
    "status": "PENDING"
  },
  "parcel-uuid-2": {
    "id": "parcel-uuid-2",
    "code": "PARCEL002",
    "status": "DELIVERED"
  }
}
```
