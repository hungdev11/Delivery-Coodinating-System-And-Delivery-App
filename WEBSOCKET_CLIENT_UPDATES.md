# WebSocket Client Updates Summary

## Overview
Updated both Android and Vue.js clients to use the correct WebSocket endpoint path `/ws/websocket`.

---

## Critical Configuration

### WebSocket Connection URL
```
wss://localweb.phuongy.works/ws/websocket
```

**Key Points:**
- ✅ Use full SockJS endpoint: `/ws/websocket` (not just `/ws`)
- ✅ Authentication: `Authorization: Bearer <USER_ID>` (not JWT token)
- ✅ Protocol: `wss://` for secure connections

---

## Client Updates

### 1. Android Client (DeliveryApp)

**File:** `DeliveryApp/app/src/main/java/com/ds/deliveryapp/ChatActivity.java`

**Configuration:**
```java
// Line 58
private static final String SERVER_WEBSOCKET_URL = "wss://localweb.phuongy.works/ws/websocket";
```

**Notes:**
- ✅ Already using correct endpoint
- ✅ Using userId for authentication
- ✅ All subscriptions configured correctly

**Subscriptions:**
- `/user/{userId}/queue/messages`
- `/user/{userId}/queue/proposal-updates`
- `/user/{userId}/queue/status-updates`
- `/user/{userId}/queue/typing`
- `/user/{userId}/queue/notifications`

---

### 2. Vue.js Client (ManagementSystem)

**File:** `ManagementSystem/src/modules/Communication/composables/useWebSocket.ts`

**Configuration:**
```typescript
// For SockJS (base URL, client appends /websocket automatically)
const wsUrl = 'https://localweb.phuongy.works/ws'

// SockJS client handles: /ws -> /ws/websocket
```

**Updated Comments:**
```typescript
/**
 * Get WebSocket URL from environment or auto-detect from current domain
 * Note: For SockJS connection, use HTTP/HTTPS URL (e.g., https://domain/ws)
 * SockJS client will automatically append /websocket and upgrade to WebSocket
 * For native WebSocket, use the full path: wss://domain/ws/websocket
 */
```

**Notes:**
- ✅ SockJS automatically appends `/websocket` to base URL
- ✅ Using userId for authentication
- ✅ All subscriptions configured correctly

**Subscriptions:**
- `/user/{userId}/queue/messages`
- `/user/{userId}/queue/status-updates`
- `/user/{userId}/queue/typing`
- `/user/{userId}/queue/notifications`

---

## Backend Kafka Consumer Fix

**File:** `BE/communication_service/src/main/java/com/ds/communication_service/infrastructure/kafka/MessageConsumer.java`

**Changes:**
- Changed all Kafka listener methods to receive DTO objects directly instead of JSON strings
- Removed manual JSON parsing with `ObjectMapper`
- Spring Kafka deserializer handles conversion automatically

**Before:**
```java
public void consumeChatMessage(@Payload String payload, ...) {
    ChatMessagePayload message = objectMapper.readValue(payload, ChatMessagePayload.class);
    // ...
}
```

**After:**
```java
public void consumeChatMessage(@Payload ChatMessagePayload message, ...) {
    // message is already deserialized
    // ...
}
```

**Benefits:**
- ✅ No more conversion errors
- ✅ Type-safe deserialization
- ✅ Automatic validation
- ✅ Cleaner code

---

## Testing Guide

Complete testing documentation created in: **`WEBSOCKET_TESTING_GUIDE.md`**

### Includes:

1. **REST API Endpoints**
   - Get conversations
   - Get messages
   - Notifications (CRUD)
   - All with example requests/responses

2. **WebSocket STOMP Endpoints**
   - Connection setup
   - Subscribe to all queues
   - Send messages
   - Typing indicators
   - Read receipts
   - Quick actions

3. **Test Scenarios**
   - Complete chat flow
   - Typing indicators
   - Message status tracking (SENT → DELIVERED → READ)
   - Quick actions on proposals
   - Notifications

4. **Client Configuration**
   - Android setup examples
   - Vue.js setup examples
   - Authentication details

5. **Testing Checklist**
   - WebSocket connection
   - Messaging
   - Status updates
   - Typing indicators
   - Notifications
   - Quick actions
   - REST API

6. **Common Issues & Solutions**
   - 400 Bad Request fixes
   - 401 Unauthorized fixes
   - Kafka consumer errors
   - And more...

---

## Quick Start Testing

### 1. Using Postman (WebSocket)

**Connect:**
```
URL: wss://localweb.phuongy.works/ws/websocket
Type: WebSocket
```

**Send CONNECT Frame:**
```
CONNECT
Authorization:Bearer <USER_ID>
accept-version:1.1,1.0
heart-beat:10000,10000

^@
```

**Subscribe to Messages:**
```
SUBSCRIBE
id:sub-0
destination:/user/<USER_ID>/queue/messages

^@
```

**Send Message:**
```
SEND
destination:/app/chat.send
content-type:application/json

{"content":"Hello!","recipientId":"<RECIPIENT_ID>"}
^@
```

---

### 2. Using REST API (Postman)

**Get Conversations:**
```http
GET https://localweb.phuongy.works/api/v1/conversations
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <USER_ID>
```

**Get Messages:**
```http
GET https://localweb.phuongy.works/api/v1/conversations/{conversationId}/messages?page=0&size=50
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <USER_ID>
```

**Get Unread Notifications:**
```http
GET https://localweb.phuongy.works/api/v1/notifications/unread
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <USER_ID>
```

---

## Environment Variables

### Android (build.gradle or local.properties)
```properties
WS_URL=wss://localweb.phuongy.works/ws/websocket
```

### Vue.js (.env)
```env
VITE_WS_URL=https://localweb.phuongy.works/ws
```

Note: Vue.js uses base URL, SockJS appends `/websocket`

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         Clients                              │
├──────────────────────────┬──────────────────────────────────┤
│  Android (DeliveryApp)   │  Vue.js (ManagementSystem)       │
│  wss://.../ws/websocket  │  https://.../ws (SockJS)         │
└──────────────────────────┴──────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Nginx Proxy                             │
│  /ws -> communication-service:21511/ws                       │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Communication Service (Spring Boot)             │
│  - WebSocket endpoint: /ws                                   │
│  - STOMP over SockJS                                         │
│  - Authentication: JWT interceptor (userId)                  │
└─────────────────────────────────────────────────────────────┘
                            │
                    ┌───────┴───────┐
                    ▼               ▼
        ┌──────────────────┐  ┌──────────────┐
        │  MySQL Database  │  │    Kafka     │
        │  - Messages      │  │  - Messages  │
        │  - Notifications │  │  - Status    │
        │  - Conversations │  │  - Typing    │
        └──────────────────┘  │  - Notifs    │
                              └──────────────┘
```

---

## Message Flow

### Sending a Message

1. **Client A** sends message via WebSocket:
   ```
   SEND /app/chat.send
   {content: "Hi!", recipientId: "user-b"}
   ```

2. **Server** processes:
   - Saves to database (status: SENT)
   - Publishes to Kafka (chat-messages topic)
   - Sends to Client A (confirmation)

3. **Kafka Consumer** receives:
   - Distributes via WebSocket to Client B

4. **Client B** receives:
   - Displays message
   - Automatically marks as DELIVERED
   - Updates status in database

5. **Client B** views message:
   - Sends read receipt
   - Status updated to READ
   - Client A receives status update

---

## Status Flow

```
SENT → DELIVERED → READ
  ↓         ↓         ↓
  DB      Kafka     WebSocket
  ↓         ↓         ↓
Status   Event    Update UI
```

---

## Testing Priority

### High Priority ✅
1. WebSocket connection
2. Send/receive messages
3. Message status updates
4. REST API for conversations
5. REST API for messages

### Medium Priority 🔶
1. Typing indicators
2. Notifications
3. Quick actions
4. Read receipts

### Low Priority 🔽
1. Reconnection handling
2. Offline message queue
3. Message editing
4. Message deletion

---

## Known Issues (Resolved)

### ✅ Issue 1: Kafka Consumer Type Conversion
**Error:** `Cannot convert from [TypingIndicator] to [String]`

**Solution:** Updated all Kafka listeners to use DTO objects directly instead of JSON strings

### ✅ Issue 2: WebSocket 400 Bad Request
**Error:** Expected HTTP 101 but got 400

**Solution:** Use full path `/ws/websocket` for SockJS endpoint

### ✅ Issue 3: Authentication Header
**Error:** 401 Unauthorized

**Solution:** Use `Authorization: Bearer <USER_ID>` (not JWT token)

---

## Next Steps

1. ✅ Test WebSocket connection with both clients
2. ✅ Test message sending/receiving
3. ✅ Verify Kafka consumers working
4. ✅ Test status updates flow
5. ✅ Test typing indicators
6. ✅ Test notifications
7. ✅ Test quick actions
8. ⏸️ Load testing with multiple concurrent users
9. ⏸️ Performance optimization
10. ⏸️ Add monitoring and metrics

---

## Support & Documentation

- **WebSocket Testing Guide:** `WEBSOCKET_TESTING_GUIDE.md`
- **Kafka Integration Guide:** `KAFKA_INTEGRATION_GUIDE.md`
- **Messenger Features:** `MESSENGER_FEATURES_IMPLEMENTATION.md`
- **Implementation Summary:** `IMPLEMENTATION_SUMMARY.md`

---

**Last Updated:** 2025-11-11
**Version:** 1.0
