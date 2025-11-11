# WebSocket Testing Guide

Complete guide for testing the Communication Service WebSocket endpoints using Postman and manual clients.

## Quick Start (TL;DR)

**"I subscribed but receive nothing!"** → You need to **SEND a message first**. Follow these steps:

### Setup (2 Postman Tabs)

```
┌─────────────────────────┐         ┌─────────────────────────┐
│   Postman Tab 1         │         │   Postman Tab 2         │
│   (User A)              │         │   (User B)              │
│   ID: 659235...892e     │         │   ID: 62b08...e1bc71    │
└─────────────────────────┘         └─────────────────────────┘
         │                                    │
         │ 1. CONNECT                         │ 1. CONNECT
         │ wss://.../ws/websocket             │ wss://.../ws/websocket
         │                                    │
         │ 2. SUBSCRIBE                       │ 2. SUBSCRIBE
         │ /user/659.../queue/messages        │ /user/62b.../queue/messages
         │                                    │
         │ 3. SEND MESSAGE ─────────────────> Server ────────────> │ 4. RECEIVE
         │    to: 62b08...                    │                    │
         │                                    │                    │
         │ 5. RECEIVE (confirmation) <────────┘                    │
         │                                                         │
         └─────────────────────────────────────────────────────────┘
```

### Steps:

1. Open **2 Postman WebSocket tabs** (User A & User B)
2. Both: Connect to `wss://localweb.phuongy.works/ws/websocket`
3. Both: Send CONNECT frame with `Authorization:Bearer <USER_ID>`
4. Both: Subscribe to `/user/<USER_ID>/queue/messages`
5. **User A: SEND a message** via `/app/chat.send` with `recipientId` = User B's ID
6. ✅ **Both tabs should receive the message!**

See [Scenario 1](#scenario-1-complete-chat-flow-step-by-step) for detailed step-by-step instructions.

---

## Copy-Paste Quick Test

### Tab 1 (User A: 62b08293-e714-45e1-9bec-a4a7e9e1bc71)

**1. CONNECT:**
```
CONNECT
Authorization:Bearer 62b08293-e714-45e1-9bec-a4a7e9e1bc71
accept-version:1.1,1.0
heart-beat:10000,10000

^@
```

**2. SUBSCRIBE:**
```
SUBSCRIBE
id:sub-0
destination:/user/62b08293-e714-45e1-9bec-a4a7e9e1bc71/queue/messages

```
(Note: Leave a blank line after `destination:...`, then Postman will add NULL byte automatically)

**3. SEND MESSAGE:**
```
SEND
destination:/app/chat.send
content-type:application/json

{"content":"Hi from User A!","recipientId":"659235bc-60c6-45b3-bab8-2adb83b0892e"}
^@
```

---

### Tab 2 (User B: 659235bc-60c6-45b3-bab8-2adb83b0892e)

**1. CONNECT:**
```
CONNECT
Authorization:Bearer 659235bc-60c6-45b3-bab8-2adb83b0892e
accept-version:1.1,1.0
heart-beat:10000,10000

^@
```

**2. SUBSCRIBE:**
```
SUBSCRIBE
id:sub-0
destination:/user/659235bc-60c6-45b3-bab8-2adb83b0892e/queue/messages

```
(Note: Leave a blank line after `destination:...`, then Postman will add NULL byte automatically)

**3. SEND MESSAGE:**
```
SEND
destination:/app/chat.send
content-type:application/json

{"content":"Hi from User B!","recipientId":"62b08293-e714-45e1-9bec-a4a7e9e1bc71"}
^@
```

---

**Note:** In Postman, `^@` represents the NULL byte. You may need to:
- Type it manually as a special character (Alt+0 on Windows numpad)
- Or use Postman's "Raw" mode and add a blank line at the end
- Or just press Enter twice after the JSON payload

**CRITICAL:** Make sure there are NO extra spaces at the end of each line! STOMP is very strict about formatting.

---

## Table of Contents
1. [WebSocket Connection](#websocket-connection)
2. [REST API Endpoints](#rest-api-endpoints)
3. [WebSocket STOMP Endpoints](#websocket-stomp-endpoints)
4. [Test Scenarios](#test-scenarios)
5. [Client Configuration](#client-configuration)
6. [Common Issues & Solutions](#common-issues--solutions)

---

## WebSocket Connection

### Connection URL
```
wss://localweb.phuongy.works/ws/websocket
```

**Important Notes:**
- Use the full SockJS endpoint path: `/ws/websocket`
- WebSocket endpoint is proxied directly (not through `/api`)
- Authentication uses `Authorization: Bearer <USER_ID>` header (not JWT token)

### Connection Flow
1. **WebSocket Handshake**: `wss://localweb.phuongy.works/ws/websocket`
2. **STOMP CONNECT**: Send CONNECT frame with Authorization header
3. **Subscribe to Topics**: Subscribe to user-specific queues
4. **Send Messages**: Send to application destinations

---

## REST API Endpoints

### 1. Get Conversations
**GET** `https://localweb.phuongy.works/api/v1/conversations`

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <USER_ID>
```

**Response:**
```json
{
  "success": true,
  "message": "Conversations retrieved successfully",
  "data": [
    {
      "id": "72de1752-c48b-4cc3-aab1-05ede184fa4a",
      "partnerId": "62b08293-e714-45e1-9bec-a4a7e9e1bc71",
      "partnerName": "User Unknown",
      "lastMessage": "hehe",
      "lastMessageTime": "2025-11-11T17:43:10.105Z",
      "unreadCount": 0
    }
  ]
}
```

---

### 2. Get Conversation Messages
**GET** `https://localweb.phuongy.works/api/v1/conversations/{conversationId}/messages`

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <USER_ID>
```

**Query Parameters:**
```
page=0
size=50
sort=sentAt,desc
```

**Response:**
```json
{
  "success": true,
  "message": "Messages retrieved successfully",
  "data": [
    {
      "id": "379fc280-a04f-4911-8afc-2ad57f9fcff5",
      "conversationId": "72de1752-c48b-4cc3-aab1-05ede184fa4a",
      "senderId": "659235bc-60c6-45b3-bab8-2adb83b0892e",
      "content": "hehe",
      "sentAt": "2025-11-11T17:43:10.105Z",
      "type": "TEXT",
      "status": "SENT",
      "deliveredAt": null,
      "readAt": null,
      "proposal": null
    }
  ]
}
```

---

### 3. Get Notifications
**GET** `https://localweb.phuongy.works/api/v1/notifications`

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <USER_ID>
```

**Query Parameters:**
```
page=0
size=20
```

**Response:**
```json
{
  "success": true,
  "message": "Notifications retrieved successfully",
  "data": {
    "content": [
      {
        "id": "notification-id",
        "userId": "user-id",
        "title": "New Message",
        "content": "You have a new message",
        "type": "MESSAGE",
        "relatedEntityId": "message-id",
        "read": false,
        "createdAt": "2025-11-11T17:43:10.105Z",
        "readAt": null
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 20,
    "number": 0
  }
}
```

---

### 4. Mark Notification as Read
**PUT** `https://localweb.phuongy.works/api/v1/notifications/{notificationId}/read`

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <USER_ID>
```

**Response:**
```json
{
  "success": true,
  "message": "Notification marked as read",
  "data": {
    "id": "notification-id",
    "userId": "user-id",
    "read": true,
    "readAt": "2025-11-11T17:45:00.000Z"
  }
}
```

---

### 5. Mark All Notifications as Read
**PUT** `https://localweb.phuongy.works/api/v1/notifications/read-all`

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <USER_ID>
```

**Response:**
```json
{
  "success": true,
  "message": "5 notifications marked as read",
  "data": 5
}
```

---

### 6. Get Unread Notifications
**GET** `https://localweb.phuongy.works/api/v1/notifications/unread`

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <USER_ID>
```

**Response:**
```json
{
  "success": true,
  "message": "Unread notifications retrieved successfully",
  "data": [
    {
      "id": "notification-id",
      "userId": "user-id",
      "title": "New Message",
      "content": "You have a new message",
      "type": "MESSAGE",
      "read": false,
      "createdAt": "2025-11-11T17:43:10.105Z"
    }
  ]
}
```

---

### 7. Get Unread Count
**GET** `https://localweb.phuongy.works/api/v1/notifications/unread/count`

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <USER_ID>
```

**Response:**
```json
{
  "success": true,
  "message": "Unread count retrieved successfully",
  "data": 3
}
```

---

### 8. Delete Notification
**DELETE** `https://localweb.phuongy.works/api/v1/notifications/{notificationId}`

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <USER_ID>
```

**Response:**
```json
{
  "success": true,
  "message": "Notification deleted successfully",
  "data": null
}
```

---

## WebSocket STOMP Endpoints

### Connection Setup (Postman)

1. **Create New WebSocket Request**
   - URL: `wss://localweb.phuongy.works/ws/websocket`
   - Type: WebSocket

2. **Configure Headers (in URL params for WebSocket handshake)**
   ```
   Authorization: Bearer <USER_ID>
   ```
   Note: Use USER_ID, not JWT token

3. **Send STOMP CONNECT Frame**
   ```
   CONNECT
   Authorization:Bearer <USER_ID>
   accept-version:1.1,1.0
   heart-beat:10000,10000

   ^@
   ```
   Note: `^@` is the NULL byte terminator (ASCII 0)

---

### Subscribe to Topics

#### 1. Messages Queue
```
SUBSCRIBE
id:sub-0
destination:/user/<USER_ID>/queue/messages

^@
```

**Important:** After subscribing, you need to **SEND a message** to receive one back. Just subscribing won't show anything until a message is sent.

**To Test Receiving:**
1. Open **TWO Postman WebSocket tabs**
2. Connect both with different user IDs
3. Subscribe both to their respective queues
4. Send a message from one user to the other (see "Send Chat Message" section below)

**Receive Format:**
```json
{
  "id": "message-id",
  "conversationId": "conversation-id",
  "senderId": "sender-id",
  "content": "Hello!",
  "sentAt": "2025-11-11T17:43:10.105Z",
  "type": "TEXT",
  "status": "SENT",
  "deliveredAt": null,
  "readAt": null,
  "proposal": null
}
```

---

#### 2. Proposal Updates Queue
```
SUBSCRIBE
id:sub-1
destination:/user/<USER_ID>/queue/proposal-updates

^@
```

**Receive Format:**
```json
{
  "proposalId": "proposal-id",
  "status": "ACCEPTED",
  "updatedAt": "2025-11-11T17:43:10.105Z",
  "note": "Accepted by user"
}
```

---

#### 3. Status Updates Queue
```
SUBSCRIBE
id:sub-2
destination:/user/<USER_ID>/queue/status-updates

^@
```

**Receive Format:**
```json
{
  "messageId": "message-id",
  "conversationId": "conversation-id",
  "status": "DELIVERED",
  "userId": "recipient-id",
  "timestamp": "2025-11-11T17:43:10.105Z"
}
```

---

#### 4. Typing Indicators Queue
```
SUBSCRIBE
id:sub-3
destination:/user/<USER_ID>/queue/typing

^@
```

**Receive Format:**
```json
{
  "conversationId": "conversation-id",
  "userId": "typing-user-id",
  "isTyping": true,
  "timestamp": 1762882989199
}
```

---

#### 5. Notifications Queue
```
SUBSCRIBE
id:sub-4
destination:/user/<USER_ID>/queue/notifications

^@
```

**Receive Format:**
```json
{
  "id": "notification-id",
  "userId": "user-id",
  "title": "New Message",
  "content": "You have a new message from John",
  "type": "MESSAGE",
  "relatedEntityId": "message-id",
  "read": false,
  "createdAt": "2025-11-11T17:43:10.105Z",
  "readAt": null
}
```

---

### Send Messages

#### 1. Send Chat Message
```
SEND
destination:/app/chat.send
content-type:application/json

{"content":"Hello World!","recipientId":"62b08293-e714-45e1-9bec-a4a7e9e1bc71","conversationId":"72de1752-c48b-4cc3-aab1-05ede184fa4a"}
^@
```

**Payload Schema:**
```json
{
  "content": "string (required)",
  "recipientId": "string (required)",
  "conversationId": "string (optional)"
}
```

---

#### 2. Send Typing Indicator
```
SEND
destination:/app/chat.typing
content-type:application/json

{"conversationId":"72de1752-c48b-4cc3-aab1-05ede184fa4a","isTyping":true,"timestamp":1762882989199}
^@
```

**Payload Schema:**
```json
{
  "conversationId": "string (required)",
  "userId": "string (set by server)",
  "isTyping": "boolean (required)",
  "timestamp": "long (required)"
}
```

---

#### 3. Mark Messages as Read
```
SEND
destination:/app/chat.read
content-type:application/json

{"messageIds":["message-id-1","message-id-2"],"conversationId":"72de1752-c48b-4cc3-aab1-05ede184fa4a"}
^@
```

**Payload Schema:**
```json
{
  "messageIds": ["string (required)"],
  "conversationId": "string (required)"
}
```

---

#### 4. Quick Action on Proposal
```
SEND
destination:/app/chat.quick-action
content-type:application/json

{"proposalId":"proposal-id","action":"ACCEPT","note":"Sounds good!","userId":"659235bc-60c6-45b3-bab8-2adb83b0892e"}
^@
```

**Payload Schema (Accept/Reject):**
```json
{
  "proposalId": "string (required)",
  "action": "ACCEPT | REJECT | POSTPONE (required)",
  "note": "string (optional)",
  "userId": "string (set by server)"
}
```

**Payload Schema (Postpone):**
```json
{
  "proposalId": "string (required)",
  "action": "POSTPONE",
  "note": "string (optional)",
  "postponeWindowStart": "2025-11-12T10:00:00Z",
  "postponeWindowEnd": "2025-11-12T12:00:00Z",
  "userId": "string (set by server)"
}
```

---

## Test Scenarios

### Scenario 1: Complete Chat Flow (Step by Step)

**Prerequisites:**
- Open **TWO Postman WebSocket tabs**
- Tab 1 = User A (659235bc-60c6-45b3-bab8-2adb83b0892e)
- Tab 2 = User B (62b08293-e714-45e1-9bec-a4a7e9e1bc71)

#### Step 1: User A Connects
In **Tab 1** (User A):
```
URL: wss://localweb.phuongy.works/ws/websocket
```

Send CONNECT frame:
```
CONNECT
Authorization:Bearer 659235bc-60c6-45b3-bab8-2adb83b0892e
accept-version:1.1,1.0
heart-beat:10000,10000

^@
```

Wait for: `CONNECTED` frame

#### Step 2: User A Subscribes
In **Tab 1** (User A):
```
SUBSCRIBE
id:sub-0
destination:/user/659235bc-60c6-45b3-bab8-2adb83b0892e/queue/messages

^@
```

#### Step 3: User B Connects
In **Tab 2** (User B):
```
URL: wss://localweb.phuongy.works/ws/websocket
```

Send CONNECT frame:
```
CONNECT
Authorization:Bearer 62b08293-e714-45e1-9bec-a4a7e9e1bc71
accept-version:1.1,1.0
heart-beat:10000,10000

^@
```

Wait for: `CONNECTED` frame

#### Step 4: User B Subscribes
In **Tab 2** (User B):
```
SUBSCRIBE
id:sub-0
destination:/user/62b08293-e714-45e1-9bec-a4a7e9e1bc71/queue/messages

^@
```

#### Step 5: User A Sends Message
In **Tab 1** (User A):
```
SEND
destination:/app/chat.send
content-type:application/json

{"content":"Hello B! This is a test message","recipientId":"62b08293-e714-45e1-9bec-a4a7e9e1bc71"}
^@
```

#### Step 6: Verify Results

**In Tab 1 (User A) - Should receive:**
```json
MESSAGE
destination:/user/659235bc-60c6-45b3-bab8-2adb83b0892e/queue/messages
content-type:application/json

{
  "id": "generated-uuid",
  "conversationId": "conversation-uuid",
  "senderId": "659235bc-60c6-45b3-bab8-2adb83b0892e",
  "content": "Hello B! This is a test message",
  "sentAt": "2025-11-11T17:43:10.105Z",
  "type": "TEXT",
  "status": "SENT",
  "deliveredAt": null,
  "readAt": null,
  "proposal": null
}
```

**In Tab 2 (User B) - Should receive:**
```json
MESSAGE
destination:/user/62b08293-e714-45e1-9bec-a4a7e9e1bc71/queue/messages
content-type:application/json

{
  "id": "generated-uuid",
  "conversationId": "conversation-uuid",
  "senderId": "659235bc-60c6-45b3-bab8-2adb83b0892e",
  "content": "Hello B! This is a test message",
  "sentAt": "2025-11-11T17:43:10.105Z",
  "type": "TEXT",
  "status": "SENT",
  "deliveredAt": null,
  "readAt": null,
  "proposal": null
}
```

**Backend logs should show:**
- `📤 Sending message to RECIPIENT: userId=62b08293-e714-45e1-9bec-a4a7e9e1bc71`
- `📤 Sending message to SENDER: userId=659235bc-60c6-45b3-bab8-2adb83b0892e`
- `✅ Message sent to both users`
- `📤 Publishing message to Kafka topic: chat-messages`

---

### Scenario 2: Typing Indicators

1. **User A starts typing**
   ```
   SEND /app/chat.typing
   {
     "conversationId": "72de1752-c48b-4cc3-aab1-05ede184fa4a",
     "isTyping": true,
     "timestamp": 1762882989199
   }
   ```

2. **Expected Results:**
   - User B receives typing indicator
   - Kafka publishes event to `typing-events` topic
   - Event distributed via WebSocket to User B

3. **User A stops typing**
   ```
   SEND /app/chat.typing
   {
     "conversationId": "72de1752-c48b-4cc3-aab1-05ede184fa4a",
     "isTyping": false,
     "timestamp": 1762882990000
   }
   ```

---

### Scenario 3: Message Status Tracking

1. **User A sends message**
   - Initial status: `SENT`
   - Database: `status = 'SENT'`

2. **User B receives message via WebSocket**
   - Status updated to: `DELIVERED`
   - Database: `status = 'DELIVERED', delivered_at = CURRENT_TIMESTAMP`
   - User A receives status update via `/user/{userId}/queue/status-updates`

3. **User B marks message as read**
   ```
   SEND /app/chat.read
   {
     "messageIds": ["message-id"],
     "conversationId": "conversation-id"
   }
   ```
   - Status updated to: `READ`
   - Database: `status = 'READ', read_at = CURRENT_TIMESTAMP`
   - User A receives status update via `/user/{userId}/queue/status-updates`

---

### Scenario 4: Quick Actions on Proposals

1. **User A (Shipper) sends proposal**
   ```
   SEND /app/chat.send
   {
     "content": "Proposal: Postpone delivery",
     "recipientId": "client-id",
     "type": "PROPOSAL"
   }
   ```

2. **User B (Client) accepts proposal**
   ```
   SEND /app/chat.quick-action
   {
     "proposalId": "proposal-id",
     "action": "ACCEPT",
     "note": "That works for me!"
   }
   ```

3. **Expected Results:**
   - Proposal status updated to "ACCEPTED"
   - User A receives proposal update via `/user/{userId}/queue/proposal-updates`
   - System creates notification for User A

---

### Scenario 5: Notifications

1. **System creates notification** (via backend logic)
   ```java
   notificationService.createNotification(
       userId,
       "New Message",
       "You have a new message from John",
       "MESSAGE",
       messageId
   );
   ```

2. **Expected Results:**
   - Notification saved to database
   - Kafka publishes to `notifications` topic
   - User receives notification via `/user/{userId}/queue/notifications`

3. **User marks notification as read (REST API)**
   ```
   PUT /api/v1/notifications/{notificationId}/read
   ```

4. **User checks unread count (REST API)**
   ```
   GET /api/v1/notifications/unread/count
   ```

---

## Client Configuration

### Android Client (DeliveryApp)

```java
// WebSocket URL
private static final String SERVER_WEBSOCKET_URL = "wss://localweb.phuongy.works/ws/websocket";

// Initialize WebSocket Manager
ChatWebSocketManager manager = new ChatWebSocketManager(
    SERVER_WEBSOCKET_URL,
    jwtToken,
    userId  // Use userId for Authorization header
);

// Connect
manager.connect();
```

**Key Points:**
- Use full SockJS endpoint: `/ws/websocket`
- Authorization header uses `userId`, not JWT
- Subscribe to all queues: messages, status-updates, typing, notifications, proposal-updates

---

### Vue.js Client (ManagementSystem)

```typescript
// WebSocket URL (SockJS will append /websocket automatically)
const wsUrl = 'https://localweb.phuongy.works/ws'

// Connect
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

const socket = new SockJS(wsUrl)
const stompClient = new Client({
  webSocketFactory: () => socket,
  connectHeaders: {
    Authorization: `Bearer ${userId}`  // Use userId, not JWT
  },
  onConnect: () => {
    // Subscribe to queues
    stompClient.subscribe(`/user/${userId}/queue/messages`, onMessage)
    stompClient.subscribe(`/user/${userId}/queue/status-updates`, onStatusUpdate)
    stompClient.subscribe(`/user/${userId}/queue/typing`, onTyping)
    stompClient.subscribe(`/user/${userId}/queue/notifications`, onNotification)
    stompClient.subscribe(`/user/${userId}/queue/proposal-updates`, onProposalUpdate)
  }
})

stompClient.activate()
```

**Key Points:**
- Use base URL (SockJS appends `/websocket`)
- Authorization header uses `userId`, not JWT
- Subscribe to all relevant queues

---

## Testing Checklist

### WebSocket Connection
- [ ] Can connect to `wss://localweb.phuongy.works/ws/websocket`
- [ ] STOMP CONNECT succeeds with `Authorization: Bearer <USER_ID>`
- [ ] Heartbeat mechanism works (10s interval)
- [ ] Reconnection works after disconnection

### Messaging
- [ ] Can send message via `/app/chat.send`
- [ ] Message received by recipient via `/user/{userId}/queue/messages`
- [ ] Message saved to database with correct status
- [ ] Kafka publishes message to `chat-messages` topic

### Status Updates
- [ ] Message status changes: SENT → DELIVERED → READ
- [ ] Status updates received via `/user/{userId}/queue/status-updates`
- [ ] Database updates `delivered_at` and `read_at` timestamps
- [ ] Kafka publishes to `message-status-events` topic

### Typing Indicators
- [ ] Typing event sent via `/app/chat.typing`
- [ ] Typing indicator received by other user
- [ ] Kafka publishes to `typing-events` topic
- [ ] Short retention (1 minute)

### Notifications
- [ ] Notification created and saved to database
- [ ] Notification received via `/user/{userId}/queue/notifications`
- [ ] Can mark as read via REST API
- [ ] Can get unread count via REST API
- [ ] Kafka publishes to `notifications` topic

### Quick Actions
- [ ] Quick action sent via `/app/chat.quick-action`
- [ ] Proposal status updated
- [ ] Proposal update received via `/user/{userId}/queue/proposal-updates`
- [ ] Notification created for proposal owner

### REST API
- [ ] Can fetch conversations
- [ ] Can fetch messages with pagination
- [ ] Can fetch notifications with pagination
- [ ] Can mark notifications as read
- [ ] All endpoints return proper `BaseResponse` format

---

## Common Issues & Solutions

### Issue 1: 400 Bad Request on WebSocket connection
**Solution:** Use full path `/ws/websocket` in URL

### Issue 2: 401 Unauthorized
**Solution:** Ensure `Authorization: Bearer <USER_ID>` header uses userId, not JWT token

### Issue 3: Messages not received
**Solution:** Check subscription destination matches: `/user/{userId}/queue/{queueName}`

### Issue 4: Kafka consumer errors
**Solution:** Ensure DTOs match between producer and consumer (use object deserialization)

### Issue 5: Typing events too frequent
**Solution:** Debounce typing events on client side (500ms-1s)

### Issue 6: Status updates not working
**Solution:** Ensure message ID is valid UUID from database

---

## Postman Collection Template

Create a Postman collection with:

1. **Environment Variables:**
   ```
   BASE_URL: https://localweb.phuongy.works
   WS_URL: wss://localweb.phuongy.works/ws/websocket
   JWT_TOKEN: <your-jwt-token>
   USER_ID: <your-user-id>
   RECIPIENT_ID: <recipient-user-id>
   CONVERSATION_ID: <conversation-id>
   ```

2. **REST Requests:** All API endpoints above

3. **WebSocket Requests:**
   - Connection
   - Subscriptions
   - Message sending
   - Status updates
   - Typing indicators
   - Quick actions

---

## Additional Resources

- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [STOMP Protocol Specification](https://stomp.github.io/stomp-specification-1.2.html)
- [SockJS Documentation](https://github.com/sockjs/sockjs-client)
- [Kafka Spring Integration](https://docs.spring.io/spring-kafka/reference/html/)

---

**Last Updated:** 2025-11-11
**Version:** 1.0
