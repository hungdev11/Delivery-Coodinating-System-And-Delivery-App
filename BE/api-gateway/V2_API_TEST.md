# API V2 Testing Guide

## Overview
This guide shows how to test the new V2 API endpoints in the API Gateway.

## Prerequisites
- API Gateway running on `http://localhost:21500`
- User Service running on `http://localhost:21501`
- Valid JWT token for authentication

## V2 Endpoints

### User Service V2

#### 1. Get Users (V2) - POST
```bash
curl -X POST http://localhost:21500/api/v2/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "page": 0,
    "size": 10,
    "sorts": [
      {
        "field": "username",
        "direction": "asc"
      }
    ]
  }'
```

#### 2. Get Current User (V2) - GET
```bash
curl -X GET http://localhost:21500/api/v2/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 3. Get User by ID (V2) - GET
```bash
curl -X GET http://localhost:21500/api/v2/users/{userId} \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 4. Get User by Username (V2) - GET
```bash
curl -X GET http://localhost:21500/api/v2/users/username/{username} \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 5. Create User (V2) - POST
```bash
curl -X POST http://localhost:21500/api/v2/users/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "password": "password123"
  }'
```

#### 6. Update User (V2) - PUT
```bash
curl -X PUT http://localhost:21500/api/v2/users/{userId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "firstName": "Updated",
    "lastName": "Name"
  }'
```

#### 7. Delete User (V2) - DELETE
```bash
curl -X DELETE http://localhost:21500/api/v2/users/{userId} \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Testing with V2 Filter System

The V2 endpoints support the enhanced filter system with pair-level operations.

### Example: Complex Filter Query
```bash
curl -X POST http://localhost:21500/api/v2/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "page": 0,
    "size": 10,
    "filters": {
      "type": "group",
      "items": [
        {
          "type": "condition",
          "field": "status",
          "operator": "EQUALS",
          "value": "ACTIVE"
        },
        {
          "type": "operator",
          "value": "AND"
        },
        {
          "type": "condition",
          "field": "age",
          "operator": "GREATER_THAN_OR_EQUAL",
          "value": 18
        },
        {
          "type": "operator",
          "value": "OR"
        },
        {
          "type": "condition",
          "field": "role",
          "operator": "EQUALS",
          "value": "ADMIN"
        }
      ]
    }
  }'
```

This represents: `status=ACTIVE AND age>=18 OR role=ADMIN`

## Frontend Testing

### ManagementSystem V2 Support

The frontend now supports V2 APIs. By default, the Users module uses V2 endpoints.

To test in the frontend:

1. Start the ManagementSystem frontend:
```bash
cd ManagementSystem
npm run dev
```

2. Navigate to the Users page
3. The frontend will automatically use V2 endpoints when `useV2Api` is set to `true`

### Toggle Between V1 and V2

In the `useUsers` composable, you can toggle between V1 and V2:

```typescript
const { useV2Api } = useUsers()

// Use V2 (default)
useV2Api.value = true

// Use V1
useV2Api.value = false
```

## Expected Responses

### Success Response
```json
{
  "result": {
    "data": [...],
    "page": {
      "page": 0,
      "size": 10,
      "totalElements": 100,
      "totalPages": 10
    }
  },
  "message": "Success"
}
```

### Error Response
```json
{
  "message": "Error description",
  "errors": []
}
```

## Verification Steps

1. **Verify V2 endpoints are accessible**:
   - Check that `/api/v2/users` returns 200 OK (with auth)
   - Check that `/api/v1/users` still works (backward compatibility)

2. **Verify V2 filter system**:
   - Test complex filters with mixed AND/OR operations
   - Verify that the backend User_service V2 endpoints are called

3. **Verify frontend integration**:
   - Check browser network tab to see requests to `/v2/users`
   - Verify responses are correctly parsed and displayed

## Troubleshooting

### 404 Not Found
- Ensure API Gateway is running
- Check that V2 controllers are properly registered
- Verify route mappings in Spring Boot

### 401 Unauthorized
- Ensure JWT token is valid
- Check token expiration
- Verify Authorization header format: `Bearer <token>`

### 500 Internal Server Error
- Check API Gateway logs
- Verify User Service is running and accessible
- Check that User Service has V2 endpoints implemented

## Notes

- V2 endpoints maintain the same authentication requirements as V1
- V2 endpoints use the same DTOs as V1 for backward compatibility
- The main difference is the route path (`/api/v2/` vs `/api/v1/`)
- V2 supports enhanced filter system (when backend services implement it)
