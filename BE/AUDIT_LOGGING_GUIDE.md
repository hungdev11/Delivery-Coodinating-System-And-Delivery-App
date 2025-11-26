# Audit Logging System Guide

## Overview

Hệ thống audit logging tự động log tất cả các operations (CREATE/UPDATE/DELETE) từ **api-gateway** và **communication-service** vào Kafka. Hệ thống hỗ trợ retry mechanism và dead letter queue cho các trường hợp failed requests.

## Architecture

```
┌──────────────┐         ┌──────────────┐         ┌─────────────┐
│ api-gateway  │────────>│    Kafka     │────────>│ audit-log-  │
│              │         │  (audit-     │         │   service   │
│ AuditLogAspect│         │   events)    │         │  (optional) │
└──────────────┘         └──────────────┘         └─────────────┘
                                │
                                │ (failed events)
                                ▼
                         ┌──────────────┐
                         │ audit-events-│
                         │     dlq      │
                         └──────────────┘

┌──────────────────┐     ┌──────────────┐
│ communication-   │────>│    Kafka     │
│    service       │     │  (audit-     │
│                  │     │   events)    │
│ AuditLogAspect   │     └──────────────┘
└──────────────────┘
```

## Components

### 1. AuditEventDto

DTO chứa thông tin về audit event:
- **OperationType**: CREATE, UPDATE, DELETE
- **Status**: SUCCESS, FAILED, PENDING
- **User Info**: userId, userRoles (từ JWT token)
- **Request Info**: httpMethod, endpoint, resourceType, resourceId
- **Response Info**: responseStatus, durationMs
- **Error Info**: errorMessage, errorStackTrace (nếu failed)

### 2. AuditLogAspect

Aspect tự động intercept tất cả controller methods và log operations:
- Chỉ log CREATE/UPDATE/DELETE (POST, PUT, PATCH, DELETE)
- Tự động extract resource info từ endpoint
- Capture request payload (truncated nếu quá lớn)
- Measure duration
- Handle errors và log failed operations

### 3. AuditEventPublisher

Publisher gửi audit events tới Kafka:
- Async send với CompletableFuture
- Auto-retry với Kafka producer retry mechanism
- Dead Letter Queue cho failed events
- Enrich với user context (userId, roles)

### 4. Kafka Topics

- **audit-events**: Main topic cho audit logs
  - Retention: 30 days
  - Partitions: 3 (partition by resourceId hoặc userId)
  
- **audit-events-dlq**: Dead Letter Queue cho failed events
  - Retention: 90 days
  - Partitions: 1

## Configuration

### api-gateway

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

### communication-service

Đã có sẵn Kafka configuration, chỉ cần thêm audit topics.

## Usage

### Automatic Logging

Hệ thống tự động log tất cả CREATE/UPDATE/DELETE operations. Không cần thêm code.

### Manual Logging (Optional)

Nếu cần log thủ công:

```java
@Autowired
private AuditEventPublisher auditEventPublisher;

public void someMethod() {
    auditEventPublisher.logOperation(
        AuditEventDto.OperationType.CREATE,
        "POST",
        "/api/v1/parcels",
        "parcel",
        parcelId,
        201,
        AuditEventDto.Status.SUCCESS,
        150L,
        requestId,
        clientIp,
        userAgent,
        requestPayload,
        null
    );
}
```

## Retry Mechanism

### Kafka Producer Retry

- **Retries**: 3 lần
- **Retry Backoff**: 1000ms
- **Idempotence**: Enabled (prevent duplicate messages)

### Dead Letter Queue

Khi publish event thất bại:
1. Event được gửi tới `audit-events-dlq`
2. Event được mark là FAILED
3. Error message và stack trace được lưu
4. Có thể retry thủ công từ DLQ sau

## Viewing Audit Logs

### Option 1: Kafka Consumer (Temporary)

```bash
# Consume from audit-events topic
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic audit-events \
  --from-beginning

# Consume from DLQ
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic audit-events-dlq \
  --from-beginning
```

### Option 2: Audit Log Service (Future)

Tạo một service riêng để:
- Consume từ `audit-events` topic
- Lưu vào database (PostgreSQL/MySQL)
- Provide REST API để query logs
- Dashboard để xem logs

## Example Audit Event

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-01-16T10:30:00",
  "operationType": "CREATE",
  "sourceService": "api-gateway",
  "userId": "72d01198-4a4e-4743-8cb8-038a9de9ea98",
  "userRoles": "CLIENT",
  "httpMethod": "POST",
  "endpoint": "/api/v1/parcels",
  "resourceType": "parcel",
  "resourceId": "123e4567-e89b-12d3-a456-426614174000",
  "requestPayload": {
    "ParcelCreateRequest": "{...}"
  },
  "responseStatus": 201,
  "status": "SUCCESS",
  "durationMs": 150,
  "requestId": "req-123",
  "clientIp": "192.168.1.1",
  "userAgent": "Mozilla/5.0..."
}
```

## Failed Request in Chain

Khi một service trong chain bị lỗi:

1. **api-gateway** log operation với status FAILED
2. Event được gửi tới Kafka (async, không block request)
3. Nếu Kafka cũng down, event được gửi tới DLQ
4. Khi service sẵn sàng, có thể:
   - Retry từ DLQ (manual hoặc automated)
   - Query DLQ để xem failed events
   - Replay events nếu cần

## Best Practices

1. **Payload Size**: Request payload được truncate nếu > 10KB để tránh Kafka message size issues
2. **Error Handling**: Tất cả errors được catch và log, không ảnh hưởng đến business logic
3. **Performance**: Async send không block request processing
4. **Retention**: 30 days cho normal events, 90 days cho failed events (DLQ)

## Future Enhancements

1. **Audit Log Service**: Service riêng để consume và lưu vào database
2. **Dashboard**: UI để xem và query audit logs
3. **Alerting**: Alert khi có nhiều failed operations
4. **Analytics**: Phân tích patterns từ audit logs
5. **Compliance**: Export logs cho compliance requirements
