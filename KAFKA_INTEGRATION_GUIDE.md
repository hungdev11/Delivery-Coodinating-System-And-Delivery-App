# Kafka Integration Guide

## Overview
This guide explains how Kafka is integrated into the Delivery System for message queuing and event streaming in the chat system.

## Architecture

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Client A  │────────>│Communication │────────>│    Kafka    │
│  (WebSocket)│         │   Service    │         │  (Broker)   │
└─────────────┘         └──────────────┘         └─────────────┘
                                │                        │
                                │                        │
                        ┌───────▼──────┐         ┌───────▼──────┐
                        │   Producer   │         │   Consumer   │
                        └──────────────┘         └──────────────┘
                                                         │
                                                         ▼
                                                  ┌─────────────┐
                                                  │   Client B  │
                                                  │  (WebSocket)│
                                                  └─────────────┘
```

## Kafka Topics

### 1. chat-messages
**Purpose**: Primary channel for message delivery

**Configuration:**
- Partitions: 3 (for parallel processing)
- Replication Factor: 1
- Retention: 7 days (604800000 ms)
- Key: conversationId (ensures message ordering per conversation)

**Message Format:**
```json
{
  "content": "Hello!",
  "recipientId": "user-123",
  "conversationId": "conv-456"
}
```

**Use Cases:**
- Guaranteed message delivery
- Message persistence during recipient offline
- Load balancing across service instances

### 2. message-status-events
**Purpose**: Track message status changes (SENT/DELIVERED/READ)

**Configuration:**
- Partitions: 3
- Replication Factor: 1
- Retention: 1 day (86400000 ms)
- Key: userId (status updates partitioned by user)

**Message Format:**
```json
{
  "messageId": "msg-789",
  "conversationId": "conv-456",
  "status": "READ",
  "userId": "user-123",
  "timestamp": "2025-11-11T22:30:00"
}
```

**Use Cases:**
- Real-time status updates to sender
- Message status history tracking
- Analytics on message engagement

### 3. typing-events
**Purpose**: Real-time typing indicators

**Configuration:**
- Partitions: 3
- Replication Factor: 1
- Retention: 1 minute (60000 ms) - SHORT-LIVED
- Key: conversationId

**Message Format:**
```json
{
  "conversationId": "conv-456",
  "userId": "user-123",
  "isTyping": true,
  "timestamp": 1699732200000
}
```

**Use Cases:**
- Show "User is typing..." indicator
- Ephemeral events (don't need long-term storage)
- High-frequency events with low retention

### 4. notifications
**Purpose**: System and user notifications

**Configuration:**
- Partitions: 3
- Replication Factor: 1
- Retention: 30 days (2592000000 ms)
- Key: userId

**Message Format:**
```json
{
  "id": "notif-111",
  "userId": "user-123",
  "title": "New Message",
  "content": "You have a new message from John",
  "type": "NEW_MESSAGE",
  "relatedEntityId": "msg-789",
  "read": false,
  "createdAt": "2025-11-11T22:30:00"
}
```

**Use Cases:**
- In-app notifications
- Push notification triggers
- Notification history

## Producer Configuration

### Spring Kafka Producer (Backend)
```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all  # Wait for all replicas
      retries: 3  # Retry on failure
      properties:
        enable.idempotence: true  # Exactly-once semantics
        max.in.flight.requests.per.connection: 5
```

### Producer Classes

**MessageProducer** (`chat-messages` topic):
```java
@Component
public class MessageProducer {
    private final KafkaTemplate<String, ChatMessagePayload> kafkaTemplate;

    public void publishMessage(String conversationId, ChatMessagePayload payload) {
        kafkaTemplate.send(KafkaConfig.CHAT_MESSAGES_TOPIC, conversationId, payload)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Message sent to Kafka. Offset: {}", result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send message to Kafka", ex);
                }
            });
    }
}
```

**EventProducer** (status, typing, notifications):
```java
@Component
public class EventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishMessageStatusUpdate(String userId, MessageStatusUpdate update) {
        kafkaTemplate.send(KafkaConfig.MESSAGE_STATUS_EVENTS_TOPIC, userId, update);
    }

    public void publishTypingEvent(String conversationId, TypingIndicator indicator) {
        kafkaTemplate.send(KafkaConfig.TYPING_EVENTS_TOPIC, conversationId, indicator);
    }

    public void publishNotification(String userId, NotificationMessage notification) {
        kafkaTemplate.send(KafkaConfig.NOTIFICATIONS_TOPIC, userId, notification);
    }
}
```

## Consumer Configuration

### Spring Kafka Consumer (Backend)
```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: communication-service-group
      auto-offset-reset: earliest  # Start from beginning if no offset
      enable-auto-commit: false  # Manual commit for reliability
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
        spring.json.trusted.packages: "com.ds.communication_service.common.dto"
```

### Consumer Class

**MessageConsumer** (`chat-messages` topic):
```java
@Component
public class MessageConsumer {
    private final SimpMessageSendingOperations messagingTemplate;
    private final MessageService messageService;
    private final EventProducer eventProducer;

    @KafkaListener(topics = KafkaConfig.CHAT_MESSAGES_TOPIC, 
                   groupId = "communication-service-group")
    public void listenChatMessages(ChatMessagePayload payload) {
        log.info("Received message from Kafka: conversationId={}", payload.getConversationId());

        // Send to recipient via WebSocket
        String recipientDestination = "/user/" + payload.getRecipientId() + "/queue/messages";
        messagingTemplate.convertAndSendToUser(
            payload.getRecipientId(),
            "/queue/messages",
            messageResponse
        );

        // Update status to DELIVERED
        messageService.updateMessageStatus(messageId, MessageStatus.DELIVERED);

        // Publish DELIVERED status event
        eventProducer.publishMessageStatusUpdate(
            payload.getSenderId(),
            MessageStatusUpdate.builder()
                .messageId(messageId)
                .status(MessageStatus.DELIVERED)
                .build()
        );
    }
}
```

## Message Flow

### 1. Send Message Flow
```
Client A                    Server                      Kafka                    Client B
   │                          │                           │                         │
   ├──[1. Send via WS]───────>│                           │                         │
   │                          ├──[2. Save to DB]──────────>                         │
   │                          ├──[3. Publish to Kafka]───>│                         │
   │                          │                           ├──[4. Store & Replicate]  │
   │                          │                           ├──[5. Consumer receives] │
   │                          │<──────────────────────────┤                         │
   │                          ├──[6. Send via WS]─────────────────────────────────>│
   │<──[7. Status: DELIVERED]─┤                           │                         │
   │                          │                           │                         │
```

### 2. Status Update Flow
```
Client B                    Server                      Kafka                    Client A
   │                          │                           │                         │
   ├──[1. Mark as READ]──────>│                           │                         │
   │                          ├──[2. Update DB]───────────>                         │
   │                          ├──[3. Publish status event]>│                         │
   │                          │                           ├──[4. Consumer receives] │
   │                          │<──────────────────────────┤                         │
   │                          ├──[5. Send status update]──────────────────────────>│
   │                          │                           │                         │
```

## Monitoring

### Kafka UI
Access at: http://localhost:8090

**Features:**
- Topic management
- Consumer group monitoring
- Message browsing
- Partition details
- Broker health

### Key Metrics to Monitor
1. **Consumer Lag**: Difference between produced and consumed messages
2. **Throughput**: Messages per second
3. **Error Rate**: Failed message processing
4. **Partition Distribution**: Even distribution across partitions

## Troubleshooting

### Consumer Not Receiving Messages
```bash
# Check if Kafka is running
docker-compose ps kafka

# Check topic creation
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Check consumer group
docker-compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group communication-service-group
```

### Messages Stuck in Topic
1. Check consumer logs for errors
2. Verify consumer group offset
3. Check topic retention settings
4. Restart consumer if necessary

### High Consumer Lag
1. Scale up consumer instances
2. Increase partition count
3. Optimize message processing logic
4. Check database performance

## Scaling Considerations

### Horizontal Scaling
- Add more partitions for higher throughput
- Deploy multiple service instances
- Kafka automatically balances partitions across consumers

### Vertical Scaling
- Increase Kafka broker resources
- Optimize batch sizes
- Tune consumer fetch properties

## Best Practices

1. **Idempotent Producers**: Enable `enable.idempotence=true`
2. **Manual Offset Commit**: Commit only after successful processing
3. **Error Handling**: Implement retry logic with exponential backoff
4. **Monitoring**: Set up alerts for consumer lag
5. **Testing**: Test failover scenarios (Kafka down, network issues)
6. **Security**: Use SSL/SASL for production deployments
7. **Retention**: Set appropriate retention based on data volume

## Production Checklist

- [ ] Increase replication factor to 3
- [ ] Enable SSL/TLS encryption
- [ ] Set up authentication (SASL)
- [ ] Configure monitoring and alerting
- [ ] Set up log aggregation
- [ ] Document disaster recovery procedures
- [ ] Load test with production-like traffic
- [ ] Configure backup and restore procedures

## References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/reference/)
- [Kafka: The Definitive Guide](https://www.confluent.io/resources/kafka-the-definitive-guide/)
