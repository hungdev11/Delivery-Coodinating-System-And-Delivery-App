# Zone Service Integration - API Gateway

## Overview

This document describes the complete integration of Zone Service routes into the API Gateway. The integration provides a unified API endpoint for all Zone Service functionality including zones, centers, routing, and addresses.

## Base URL

```
http://localhost:8080/api/v1
```

## Integrated Endpoints

### 1. Zone Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/zones` | List all zones (paginated) |
| GET | `/zones/{id}` | Get zone by ID |
| GET | `/zones/code/{code}` | Get zone by code |
| GET | `/zones/center/{centerId}` | Get zones by center ID |
| POST | `/zones` | Create new zone |
| PUT | `/zones/{id}` | Update zone |
| DELETE | `/zones/{id}` | Delete zone |

### 2. Center Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/centers` | List all centers (paginated) |
| GET | `/centers/{id}` | Get center by ID |
| GET | `/centers/code/{code}` | Get center by code |
| POST | `/centers` | Create new center |
| PUT | `/centers/{id}` | Update center |
| DELETE | `/centers/{id}` | Delete center |

### 3. Routing Services

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/routing/route` | Calculate route between waypoints |
| GET | `/routing/osrm-status` | Get OSRM service status |

### 4. Address Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/addresses` | List all addresses (paginated) |
| GET | `/addresses/{id}` | Get address by ID |
| GET | `/addresses/nearest` | Find nearest addresses to point |
| POST | `/addresses` | Create new address |
| PUT | `/addresses/{id}` | Update address |
| DELETE | `/addresses/{id}` | Delete address |
| POST | `/addresses/batch` | Batch import addresses |
| GET | `/addresses/segments/{segmentId}` | Get addresses by road segment |
| GET | `/addresses/zones/{zoneId}` | Get addresses by zone |

### 5. Health Check

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/zone/health` | Zone service health status |

## Implementation Details

### Architecture

The integration follows a proxy pattern where the API Gateway:

1. **Receives requests** from clients
2. **Forwards requests** to Zone Service via WebClient
3. **Returns responses** from Zone Service to clients
4. **Handles errors** and service unavailability gracefully

### Key Components

#### 1. ZoneProxyController
- **Location**: `com.ds.gateway.application.controllers.v1.ZoneProxyController`
- **Purpose**: REST controller that exposes all Zone Service endpoints
- **Features**:
  - Query parameter extraction and forwarding
  - Request/response logging
  - Error handling via service client

#### 2. IZoneServiceClient Interface
- **Location**: `com.ds.gateway.common.interfaces.IZoneServiceClient`
- **Purpose**: Contract defining all Zone Service client methods
- **Methods**: 25+ methods covering all Zone Service functionality

#### 3. ZoneServiceClient Implementation
- **Location**: `com.ds.gateway.business.v1.services.ZoneServiceClient`
- **Purpose**: WebClient-based implementation for calling Zone Service
- **Features**:
  - Async/reactive programming with CompletableFuture
  - Error mapping to ServiceUnavailableException
  - Query parameter handling
  - JSON request/response handling

### Request/Response Flow

```
Client Request → API Gateway → Zone Service → API Gateway → Client Response
```

1. **Client** sends HTTP request to API Gateway
2. **ZoneProxyController** receives request and extracts parameters
3. **ZoneServiceClient** forwards request to Zone Service via WebClient
4. **Zone Service** processes request and returns response
5. **ZoneServiceClient** receives response and maps errors if needed
6. **ZoneProxyController** returns response to client

### Error Handling

- **Service Unavailable**: Maps to `ServiceUnavailableException` when Zone Service is down
- **Network Errors**: Handled by WebClient and mapped to appropriate exceptions
- **Validation Errors**: Passed through from Zone Service to client
- **Timeout**: Handled by WebClient configuration

### Query Parameter Support

All list endpoints support query parameters:
- **Pagination**: `page`, `size`, `limit`
- **Search**: `search`, `code`
- **Filtering**: `addressType`, `segmentId`, `zoneId`, `centerId`
- **Location**: `lat`, `lon`, `maxDistance` (for nearest addresses)

### Example Usage

#### Calculate Route
```bash
curl -X POST http://localhost:8080/api/v1/routing/route \
  -H "Content-Type: application/json" \
  -d '{
    "waypoints": [
      {"lat": 10.8505, "lon": 106.7718},
      {"lat": 10.8623, "lon": 106.8032}
    ]
  }'
```

#### Find Nearest Addresses
```bash
curl "http://localhost:8080/api/v1/addresses/nearest?lat=10.8505&lon=106.7717&limit=5&maxDistance=1000"
```

#### List Zones
```bash
curl "http://localhost:8080/api/v1/zones?page=0&size=10&search=North"
```

## Configuration

### WebClient Configuration
The Zone Service WebClient is configured in `WebClientConfig` with:
- Base URL: Zone Service endpoint
- Timeout settings
- Error handling
- Connection pooling

### Security
- All endpoints inherit API Gateway security configuration
- Authentication/authorization handled at gateway level
- Zone Service calls are internal (no additional auth needed)

## Monitoring and Logging

### Logging
- All requests are logged with endpoint and parameters
- Service calls include debug-level logging
- Error scenarios are logged with full context

### Health Checks
- Zone service health available at `/api/v1/zone/health`
- Integrates with overall API Gateway health monitoring

## Testing

### Manual Testing
```bash
# Test zone service health
curl http://localhost:8080/api/v1/zone/health

# Test routing
curl -X POST http://localhost:8080/api/v1/routing/route \
  -H "Content-Type: application/json" \
  -d '{"waypoints": [{"lat": 10.8505, "lon": 106.7718}, {"lat": 10.8623, "lon": 106.8032}]}'

# Test address search
curl "http://localhost:8080/api/v1/addresses?search=PTIT&addressType=SCHOOL"
```

### Integration Testing
- All endpoints can be tested through API Gateway
- Responses should match Zone Service documentation
- Error scenarios should be handled gracefully

## Dependencies

### Required Services
- **Zone Service**: Must be running on configured port
- **Database**: Zone Service database must be accessible
- **OSRM**: Required for routing functionality

### API Gateway Dependencies
- Spring Boot WebFlux (for WebClient)
- Spring Boot Web (for REST controllers)
- Lombok (for logging and utilities)

## Future Enhancements

### Planned Features
1. **Caching**: Add response caching for frequently accessed data
2. **Rate Limiting**: Implement rate limiting per endpoint
3. **Metrics**: Add detailed metrics and monitoring
4. **Circuit Breaker**: Implement circuit breaker pattern for resilience
5. **Load Balancing**: Support multiple Zone Service instances

### Performance Optimizations
1. **Connection Pooling**: Optimize WebClient connection settings
2. **Async Processing**: Further async optimizations
3. **Response Compression**: Add response compression
4. **Request Batching**: Batch multiple requests when possible

## Troubleshooting

### Common Issues

#### Zone Service Unavailable
- **Symptom**: 503 Service Unavailable errors
- **Solution**: Check Zone Service status and connectivity

#### Routing Failures
- **Symptom**: "NoRoute" responses
- **Solution**: Verify OSRM service is running and coordinates are valid

#### Address Search Issues
- **Symptom**: Empty results or errors
- **Solution**: Check database connectivity and geohash indexing

### Debug Steps
1. Check API Gateway logs for request details
2. Verify Zone Service is running and accessible
3. Test Zone Service endpoints directly
4. Check database connectivity
5. Verify OSRM service status

## Documentation References

- [Zone Service Routes](../zone_service/.docs/route/README.md)
- [Routing API](../zone_service/.docs/route/routing.md)
- [Address API](../zone_service/.docs/route/addresses.md)
- [Zone API](../zone_service/.docs/route/zones.md)
- [Center API](../zone_service/.docs/route/centers.md)

---

**Last Updated**: 2025-01-15  
**Version**: 1.0.0  
**Status**: Complete
