# Enriched Session Routes (API Gateway - Aggregated)

Base URL: `http://localhost:<port>/api/v1/sessions`

**Note:** This endpoint aggregates data from multiple services (session-service, parcel-service, delivery-proofs) to provide complete session information in a single call.

## Endpoint

### Get Enriched Session
- `GET /sessions/{sessionId}/enriched` - Get session with full assignment details including parcel info and proofs

**Path Parameters:**
- `sessionId` (UUID, required) - The session ID

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "deliveryManId": "driver-id",
    "status": "IN_PROGRESS",
    "startTime": "2025-12-23T00:00:00",
    "endTime": null,
    "totalTasks": 5,
    "completedTasks": 2,
    "failedTasks": 0,
    "deliveryMan": {
      "name": "Nguyen Van A",
      "vehicleType": "MOTORBIKE",
      "capacityKg": 50.0,
      "phone": "+84901234567",
      "email": "driver@example.com"
    },
    "assignments": [
      {
        "id": "assignment-uuid",
        "assignmentId": "assignment-uuid",
        "sessionId": "session-uuid",
        "parcelId": "parcel-uuid",
        "status": "COMPLETED",
        "failReason": null,
        "scanedAt": "2025-12-23T00:00:00",
        "updatedAt": "2025-12-23T01:00:00",
        "completedAt": "2025-12-23T01:00:00",
        "parcelCode": "P123456",
        "deliveryType": "STANDARD",
        "receiverName": "Tran Thi B",
        "receiverId": "receiver-uuid",
        "receiverPhone": "+84987654321",
        "deliveryLocation": "123 Main St, District 1, Ho Chi Minh City",
        "value": 500000.00,
        "weight": 2.5,
        "lat": 10.762622,
        "lon": 106.660172,
        "proofs": [
          {
            "id": "proof-uuid",
            "type": "DELIVERED",
            "mediaUrl": "https://cloudinary.com/...",
            "confirmedBy": "driver-id",
            "createdAt": "2025-12-23T01:00:00"
          }
        ]
      }
    ]
  },
  "message": null
}
```

## Usage Example

```bash
curl -X GET "http://localhost:8080/api/v1/sessions/123e4567-e89b-12d3-a456-426614174000/enriched" \
  -H "Authorization: Bearer <token>"
```

## Comparison with Basic Endpoint

| Feature | Basic Endpoint (`/sessions/{id}`) | Enriched Endpoint (`/sessions/{id}/enriched`) |
|---------|-----------------------------------|-----------------------------------------------|
| Shipper Info | ✅ Basic (ID only) | ✅ Full (name, vehicle, phone, email) |
| Assignment Info | ✅ Basic (id, parcelId, status) | ✅ Full (all assignment fields) |
| Parcel Info | ❌ Not included | ✅ Complete (receiver, location, value, weight, coordinates) |
| Proofs | ❌ Not included | ✅ All proofs with media URLs |
| API Calls | 1 call | Aggregates multiple calls internally |

## Performance Notes

- The enriched endpoint makes multiple parallel calls to:
  - Session Service (1 call)
  - Parcel Service (N calls for N parcels)
  - Delivery Proofs Service (N calls for N assignments)
- Use the basic endpoint if you only need session structure
- Use the enriched endpoint when you need complete details for display
