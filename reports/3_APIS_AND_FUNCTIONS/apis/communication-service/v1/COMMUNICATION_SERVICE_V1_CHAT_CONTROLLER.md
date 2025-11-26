# API Documentation: Chat Controller (v1) - WebSocket Endpoints

**Base Path:** WebSocket connections via `/ws`

This controller handles WebSocket-based real-time messaging operations for the Communication Service. It processes messages, typing indicators, read receipts, and quick actions through WebSocket connections.

## Overview

The Chat Controller uses Spring WebSocket (STOMP) to provide real-time bidirectional communication. Clients connect via WebSocket and send messages to specific message mappings. The server broadcasts messages to appropriate recipients.

## WebSocket Connection

**Connection URL:** `ws://host:port/ws`

**STOMP Endpoints:**
- Subscribe to messages: `/user/{userId}/queue/messages`
- Subscribe to typing indicators: `/user/{userId}/queue/typing`
- Subscribe to read receipts: `/user/{userId}/queue/read-receipts`
- Subscribe to shipper session messages: `/topic/shipper/{shipperId}/session-messages`

## Message Mappings

| Mapping | Description | Payload | Response |
|---------|-------------|---------|----------|
| `/app/chat.send` | Send a message | `ChatMessagePayload` | Broadcasts to recipient and sender |
| `/app/chat.typing` | Send typing indicator | `TypingIndicator` | Broadcasts to conversation partner |
| `/app/chat.read` | Mark messages as read | `ReadReceiptPayload` | Updates message status |
| `/app/chat.quick-action` | Handle quick action on proposals | `QuickActionRequest` | Processes proposal response |

## Message Details

### /app/chat.send

Send a message to another user. The message is saved to the database and broadcast to both the recipient and sender for immediate confirmation.

**Payload:**
```json
{
  "recipientId": "user-uuid",
  "conversationId": "conversation-uuid",
  "content": "Hello, this is a message",
  "messageType": "TEXT",
  "parcelId": "optional-parcel-uuid"
}
```

**Response:** Message is broadcast to:
- Recipient: `/user/{recipientId}/queue/messages`
- Sender: `/user/{senderId}/queue/messages`
- Shipper session (if recipient is shipper): `/topic/shipper/{shipperId}/session-messages`

**Authentication:** Required (JWT token in WebSocket handshake)

### /app/chat.typing

Send a typing indicator to show that the user is currently typing a message.

**Payload:**
```json
{
  "conversationId": "conversation-uuid",
  "isTyping": true
}
```

**Response:** Typing indicator is broadcast to conversation partner via Kafka

**Authentication:** Required (JWT token in WebSocket handshake)

### /app/chat.read

Mark one or more messages as read by the current user.

**Payload:**
```json
{
  "messageIds": ["message-uuid-1", "message-uuid-2"],
  "conversationId": "conversation-uuid"
}
```

**Response:** Message status is updated in the database

**Authentication:** Required (JWT token in WebSocket handshake)

### /app/chat.quick-action

Handle quick actions on proposals (Accept, Reject, Postpone). This enables 2-touch interaction for shippers.

**Payload:**
```json
{
  "proposalId": "proposal-uuid",
  "action": "ACCEPT",
  "note": "Optional note",
  "postponeWindowStart": "2024-01-01T10:00:00Z",
  "postponeWindowEnd": "2024-01-01T12:00:00Z"
}
```

**Action Types:**
- `ACCEPT`: Approve the proposal
- `REJECT`: Decline the proposal
- `POSTPONE`: Request to postpone (requires windowStart and windowEnd)

**Response:** Proposal response is processed and notifications are sent

**Authentication:** Required (JWT token in WebSocket handshake)

## Related Documentation

- [Conversation API Controller](COMMUNICATION_SERVICE_V1_CONVERSATION_API_CONTROLLER.md) - REST endpoints for conversations
- [Proposal Controller](COMMUNICATION_SERVICE_V1_PROPOSAL_CONTROLLER.md) - REST endpoints for proposals
- [Communication Service Architecture](../../../../2_BACKEND/2_COMMUNICATION_SERVICE.md) - Service architecture

---

**Navigation**: [← Back to Communication Service](README.md) | [↑ APIs and Functions](../../../README.md) | [↑ Report Index](../../../../README.md)
