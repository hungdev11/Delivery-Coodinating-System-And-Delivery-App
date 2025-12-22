# Delivery Proof Routes (API Gateway - Proxy)

Base URL: `http://localhost:<port>/api/v1/delivery-proofs`

**Note:** These endpoints proxy requests to the Session Service. See [Delivery Proof Service Routes](../../session-service/.docs/route/proofs.md) for detailed documentation.

## Endpoints

### Get Proofs by Assignment ID
- `GET /delivery-proofs/assignments/{assignmentId}` - Get all proofs for a specific assignment

**Path Parameters:**
- `assignmentId` (UUID, required) - The assignment ID

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "type": "DELIVERED",
      "mediaUrl": "https://cloudinary.com/...",
      "confirmedBy": "driver-id",
      "createdAt": "2025-12-23T00:00:00"
    }
  ],
  "message": null
}
```

### Get Proofs by Parcel ID
- `GET /delivery-proofs/parcels/{parcelId}` - Get all proofs for a specific parcel (from all assignments)

**Path Parameters:**
- `parcelId` (String, required) - The parcel ID

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "type": "DELIVERED",
      "mediaUrl": "https://cloudinary.com/...",
      "confirmedBy": "driver-id",
      "createdAt": "2025-12-23T00:00:00"
    }
  ],
  "message": null
}
```

## Usage Examples

### Get proofs for an assignment
```bash
curl -X GET "http://localhost:8080/api/v1/delivery-proofs/assignments/123e4567-e89b-12d3-a456-426614174000" \
  -H "Authorization: Bearer <token>"
```

### Get all proofs for a parcel
```bash
curl -X GET "http://localhost:8080/api/v1/delivery-proofs/parcels/383d21c6-9fa4-4d85-978d-6845da6ad88d" \
  -H "Authorization: Bearer <token>"
```

For request/response formats, see [Session Service documentation](../../session-service/.docs/route/proofs.md).
