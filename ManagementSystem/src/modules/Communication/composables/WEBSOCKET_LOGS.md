# 📊 WebSocket Logging Guide

## What You'll See in Console

All WebSocket logs are **only visible in development mode** and automatically tagged with context.

### 🎯 Log Format

```
[timestamp] LEVEL [CONTEXT]: Message {data}
```

Example:
```
[2025-11-12T14:57:54.456Z] INFO [WEBSOCKET]: 🔌 Connecting to WebSocket {"userId": "..."}
```

---

## 📤 SENDING Logs (What you send to server)

### 1. **Sending Message**
```javascript
[INFO] [WEBSOCKET:SENDER]: 📤 SENDING MESSAGE
{
  destination: "/app/chat.send",
  fullPayload: { conversationId, content, senderId, ... },
  bodyString: '{"conversationId":"...","content":"hello",...}',
  payloadPreview: {
    conversationId: "72de1752-...",
    content: "hello",
    senderId: "659235bc-..."
  }
}

[INFO] [WEBSOCKET:SENDER]: ✅ Message sent successfully
{
  destination: "/app/chat.send",
  timestamp: 1762959540000
}
```

### 2. **Sending Typing Indicator**
```javascript
[INFO] [WEBSOCKET:SENDER]: 📤 SENDING TYPING INDICATOR
{
  destination: "/app/chat.typing",
  payload: { conversationId, isTyping, timestamp },
  bodyString: '{"conversationId":"...","isTyping":true,...}'
}

[INFO] [WEBSOCKET:SENDER]: ✅ Typing indicator sent
```

### 3. **Marking Messages as Read**
```javascript
[INFO] [WEBSOCKET:SENDER]: 📤 SENDING MARK AS READ
{
  destination: "/app/chat.read",
  payload: { messageIds: [...], conversationId },
  bodyString: '{"messageIds":[...],...}',
  messageCount: 3,
  conversationId: "..."
}

[INFO] [WEBSOCKET:SENDER]: ✅ Mark as read sent
```

### 4. **Sending Quick Action**
```javascript
[INFO] [WEBSOCKET:SENDER]: 📤 SENDING QUICK ACTION
{
  destination: "/app/chat.quick-action",
  payload: { proposalId, action, ... },
  bodyString: '{"proposalId":"...","action":"ACCEPT",...}',
  action: "ACCEPT",
  proposalId: "..."
}

[INFO] [WEBSOCKET:SENDER]: ✅ Quick action sent
```

---

## 📥 RECEIVING Logs (What you receive from server)

### 1. **Receiving Message**
```javascript
[INFO] [WEBSOCKET:SUBSCRIPTION]: 📥 RECEIVED MESSAGE
{
  destination: "/user/queue/messages",
  messageId: "sub-0",
  subscription: "sub-0",
  rawBody: '{"id":"...","content":"hello",...}'
}

[INFO] [WEBSOCKET:SUBSCRIPTION]: 📨 Parsed message data
{
  fullData: { id, conversationId, senderId, content, ... },
  preview: {
    id: "4bc1b8ea-3616-...",
    senderId: "659235bc-...",
    content: "hello"
  }
}
```

### 2. **Receiving Status Update**
```javascript
[INFO] [WEBSOCKET:SUBSCRIPTION]: 📥 RECEIVED STATUS UPDATE
{
  destination: "/user/queue/status-updates",
  rawBody: '{"userId":"...","status":"online",...}'
}

[INFO] [WEBSOCKET:SUBSCRIPTION]: 📊 Parsed status update
{
  userId: "...",
  status: "online",
  timestamp: 1762959540000
}
```

### 3. **Receiving Typing Indicator**
```javascript
[INFO] [WEBSOCKET:SUBSCRIPTION]: 📥 RECEIVED TYPING INDICATOR
{
  destination: "/user/queue/typing",
  rawBody: '{"userId":"...","isTyping":true,...}'
}

[INFO] [WEBSOCKET:SUBSCRIPTION]: 📝 Parsed typing indicator
{
  userId: "...",
  conversationId: "...",
  isTyping: true,
  timestamp: 1762959540000
}
```

### 4. **Receiving Notification**
```javascript
[INFO] [WEBSOCKET:SUBSCRIPTION]: 📥 RECEIVED NOTIFICATION
{
  destination: "/user/queue/notifications",
  rawBody: '{"id":"...","type":"NEW_MESSAGE",...}'
}

[INFO] [WEBSOCKET:SUBSCRIPTION]: 🔔 Parsed notification
{
  id: "...",
  type: "NEW_MESSAGE",
  title: "New Message",
  message: "You have a new message",
  timestamp: 1762959540000
}
```

---

## 🔌 Connection Logs

### Connection Process
```javascript
[INFO] [WEBSOCKET]: 🔌 Connecting to WebSocket
{ userId: "659235bc-..." }

[DEBUG] [WEBSOCKET:CONNECTION]: Creating STOMP client
{ userId: "659235bc-..." }

[INFO] [WEBSOCKET:CONNECTION]: Connecting to WebSocket
{ url: "https://..../ws", userId: "..." }

[INFO] [WEBSOCKET:CONNECTION]: ✅ WebSocket connected
{ userId: "...", headers: {...} }
```

### Subscription Setup
```javascript
[INFO] [WEBSOCKET:SUBSCRIPTION]: Setting up subscriptions
{
  hasMessageCallback: true,
  hasStatusCallback: false,
  hasTypingCallback: false,
  hasNotificationCallback: false
}

[INFO] [WEBSOCKET:SUBSCRIPTION]: 📡 Subscribing to messages
{ destination: "/user/queue/messages" }

[INFO] [WEBSOCKET:SUBSCRIPTION]: ✅ Subscribed to /user/queue/messages
```

### Connection Complete
```javascript
[INFO] [WEBSOCKET]: ✅ WebSocket fully connected
{ subscriptions: 1 }
```

---

## 🐛 Error Logs

### Send Errors
```javascript
[ERROR] [WEBSOCKET:SENDER]: ❌ Failed to send message
ReferenceError: client is not connected
```

### Receive Errors
```javascript
[ERROR] [WEBSOCKET:SUBSCRIPTION]: ❌ Failed to parse message
SyntaxError: Unexpected token in JSON

[ERROR] [WEBSOCKET:SUBSCRIPTION]: Raw message body
'{"invalid json'
```

### Connection Errors
```javascript
[ERROR] [WEBSOCKET:CONNECTION]: ❌ Failed to create STOMP client
ReferenceError: global is not defined

[WARN] [WEBSOCKET]: Failed to connect WebSocket {}
```

---

## 🔍 How to Filter Logs in Browser Console

### Filter by Context
```javascript
// Show only connection logs
[WEBSOCKET:CONNECTION]

// Show only subscription logs (receiving)
[WEBSOCKET:SUBSCRIPTION]

// Show only sender logs (sending)
[WEBSOCKET:SENDER]

// Show all WebSocket logs
[WEBSOCKET
```

### Filter by Action
```javascript
// Show only sent messages
📤 SENDING

// Show only received messages
📥 RECEIVED

// Show only errors
❌
```

### Filter by Destination
```javascript
// Show only chat messages
/app/chat.send
/user/queue/messages

// Show only typing indicators
/app/chat.typing
/user/queue/typing
```

---

## 📋 Complete Message Flow Example

When you send "hello" and receive a response:

```javascript
// 1. YOU SEND
[INFO] [WEBSOCKET:SENDER]: 📤 SENDING MESSAGE
{
  destination: "/app/chat.send",
  fullPayload: {
    conversationId: "72de1752-c48b-4cc3-aab1-05ede184fa4a",
    content: "hello",
    senderId: "659235bc-60c6-45b3-bab8-2adb83b0892e"
  },
  bodyString: '{"conversationId":"72de1752-c48b-4cc3-aab1-05ede184fa4a",...}'
}

[INFO] [WEBSOCKET:SENDER]: ✅ Message sent successfully
{ destination: "/app/chat.send", timestamp: 1762959540000 }

// 2. SERVER PROCESSES (not visible in client logs)

// 3. YOU RECEIVE (your own message echoed back)
[INFO] [WEBSOCKET:SUBSCRIPTION]: 📥 RECEIVED MESSAGE
{
  destination: "/user/queue/messages",
  messageId: "sub-0",
  rawBody: '{"id":"4bc1b8ea-3616-44ca-8019-da48bfe0baea",...}'
}

[INFO] [WEBSOCKET:SUBSCRIPTION]: 📨 Parsed message data
{
  fullData: {
    id: "4bc1b8ea-3616-44ca-8019-da48bfe0baea",
    conversationId: "72de1752-c48b-4cc3-aab1-05ede184fa4a",
    senderId: "659235bc-60c6-45b3-bab8-2adb83b0892e",
    content: "hello",
    sentAt: "2025-11-12T14:59:10.894Z",  // Server adds timestamp
    type: "TEXT",
    status: "SENT"
  },
  preview: {
    id: "4bc1b8ea-3616-44ca-8019-da48bfe0baea",
    senderId: "659235bc-60c6-45b3-bab8-2adb83b0892e",
    content: "hello"
  }
}

// 4. YOUR CALLBACK PROCESSES THE MESSAGE
// (adds to UI, updates conversation, etc.)
```

---

## 💡 Tips

1. **Keep Console Open** - Filter logs in real-time to debug issues

2. **Check Timestamps** - Compare send and receive timestamps to measure latency

3. **Verify Destinations** - Make sure you're sending/receiving from correct endpoints

4. **Watch for Errors** - Red `❌` logs indicate problems

5. **Check Full Payload** - The logs show both preview and full data for inspection

6. **Production Mode** - All these logs are automatically disabled in production!

---

## 🚨 Common Issues

### "sentAt is null"
**Fixed!** The client now handles null `sentAt` by using current timestamp.

### "Message not added to pool"
Check the `addMessage` function logs. If `sentAt` is null, it's now automatically set.

### "Duplicate messages"
The `addMessage` function checks for duplicates and removes optimistic messages.

---

**Happy debugging! 🎉**
