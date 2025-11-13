# WebSocket Debugging Guide

## Quick Diagnostic Checklist

### ✅ What You've Done Successfully:
1. Connected to WebSocket (`Connected` indicator shows green)
2. Sent CONNECT frame with Authorization header
3. Subscribed to `/user/{userId}/queue/messages`
4. Sent messages via `/app/chat.send`

### ❌ What's Missing:
You're not receiving MESSAGE frames back. Let's diagnose why.

---

## Debugging Steps

### Step 1: Check Backend Logs

Look for these log messages in `communication-service` container:

```bash
docker-compose logs -f communication-service
```

**Expected logs when sending a message:**
```
📥 Tin nhắn nhận được: Từ 62b08293-e714-45e1-9bec-a4a7e9e1bc71 -> Tới 659235bc-60c6-45b3-bab8-2adb83b0892e
✅ Message saved to database with status SENT. MessageId: <uuid>
📤 Publishing message to Kafka topic: chat-messages
📤 Sending message to RECIPIENT: userId=659235bc-60c6-45b3-bab8-2adb83b0892e
📤 Sending message to SENDER: userId=62b08293-e714-45e1-9bec-a4a7e9e1bc71
✅ Message <uuid> sent to both users
```

**If you see errors:**
- Check if Principal is null (authentication issue)
- Check if WebSocket session is registered
- Check if message broker is configured

---

### Step 2: Check WebSocket Session Registration

The backend needs to know which WebSocket session belongs to which user.

**Add this log to check:**
```bash
# In the logs, look for:
"WebSocket session registered for user: <userId>"
```

If you don't see this, the WebSocket interceptor might not be setting the Principal correctly.

---

### Step 3: Test with REST API First

Before testing WebSocket, verify the backend works via REST API:

```bash
# Get conversations (should work)
curl -X GET "https://localweb.phuongy.works/api/v1/conversations" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "X-User-Id: 62b08293-e714-45e1-9bec-a4a7e9e1bc71"
```

If REST API works but WebSocket doesn't, the issue is in WebSocket configuration.

---

### Step 4: Enable WebSocket Debug Logging

Add this to `communication_service/src/main/resources/application.yaml`:

```yaml
logging:
  level:
    org.springframework.messaging: DEBUG
    org.springframework.web.socket: DEBUG
    com.ds.communication_service: DEBUG
```

This will show:
- WebSocket connection attempts
- STOMP frame processing
- Message broker routing
- User session mapping

---

### Step 5: Check Postman Configuration

**Ensure NULL byte is sent correctly:**

In Postman, after your STOMP frame content, you must include the NULL byte terminator.

**Option 1: Type it manually**
- On Windows: Hold `Alt` and type `0` on the numpad
- Result: `^@` should appear

**Option 2: Just add blank line**
- After your JSON payload, press Enter twice
- Postman should handle the NULL byte automatically

**Example:**
```
SEND
destination:/app/chat.send
content-type:application/json

{"content":"Test","recipientId":"659235bc-60c6-45b3-bab8-2adb83b0892e"}

```
(Note: blank line after JSON)

---

### Step 6: Common Issues & Solutions

#### Issue 1: No MESSAGE frames received after sending

**Possible Causes:**
1. Principal not set (authentication failed)
2. User session not registered with message broker
3. Subscription destination doesn't match send destination
4. WebSocket session disconnected

**Solution:**
Check backend logs for:
```
📤 Sending message to RECIPIENT: userId=...
📤 Sending message to SENDER: userId=...
```

If you see these logs but still no message in Postman:
- Check if Postman's WebSocket connection is still active
- Try reconnecting and resubscribing
- Check if heartbeat is working (should see HEARTBEAT frames every 10s)

---

#### Issue 2: Authentication Fails

**Symptoms:**
- 401 error on WebSocket handshake
- "No principal" errors in logs

**Solution:**
Ensure CONNECT frame includes:
```
CONNECT
Authorization:Bearer <USER_ID>  ← Use USER_ID, not JWT token
accept-version:1.1,1.0
heart-beat:10000,10000

^@
```

---

#### Issue 3: Wrong Subscription Destination

**Symptoms:**
- Messages sent but not received
- No errors in logs

**Solution:**
Ensure subscription matches exactly:
```
SUBSCRIBE
id:sub-0
destination:/user/62b08293-e714-45e1-9bec-a4a7e9e1bc71/queue/messages  ← Must match EXACT user ID

^@
```

---

#### Issue 4: Kafka Consumer Not Processing

**Symptoms:**
- Logs show "📤 Publishing message to Kafka"
- But no "📥 Received message from Kafka"

**Solution:**
Check Kafka consumer is running:
```bash
docker-compose logs -f communication-service | grep Kafka
```

Should see:
```
Kafka consumer started for topic: chat-messages
📥 Received message from Kafka. Topic: chat-messages, Partition: 2, Offset: 1
```

If not, check Kafka is running:
```bash
docker-compose ps kafka
```

---

### Step 7: Test End-to-End Flow

**Full test with 2 users:**

1. **Open 2 Postman WebSocket tabs**

2. **Tab 1 (User A: 62b08293-e714-45e1-9bec-a4a7e9e1bc71)**
   - Connect
   - CONNECT with Authorization:Bearer 62b08293-e714-45e1-9bec-a4a7e9e1bc71
   - SUBSCRIBE to /user/62b08293-e714-45e1-9bec-a4a7e9e1bc71/queue/messages
   - SEND message to 659235bc-60c6-45b3-bab8-2adb83b0892e

3. **Tab 2 (User B: 659235bc-60c6-45b3-bab8-2adb83b0892e)**
   - Connect
   - CONNECT with Authorization:Bearer 659235bc-60c6-45b3-bab8-2adb83b0892e
   - SUBSCRIBE to /user/659235bc-60c6-45b3-bab8-2adb83b0892e/queue/messages
   - Wait for message from User A

4. **Expected Result:**
   - **Tab 1**: Receives MESSAGE frame (confirmation to sender)
   - **Tab 2**: Receives MESSAGE frame (message to recipient)
   - **Backend logs**: Show both sends

---

### Step 8: Check Message Broker State

**Verify Spring's SimpleBroker is working:**

The WebSocket config uses Spring's `SimpleBroker` for `/queue` and `/topic` destinations.

**Expected behavior:**
- When you send to `/app/chat.send`, ChatController processes it
- ChatController calls `messagingTemplate.convertAndSendToUser(userId, "/queue/messages", message)`
- SimpleBroker routes to `/user/{userId}/queue/messages`
- Client subscribed to that destination receives the message

**If this doesn't work:**
- Check if `webSocketTaskScheduler` bean is created
- Check if heartbeat is configured (should be 10000ms)
- Check if user destination prefix is `/user` (it is in config)

---

### Step 9: Manual WebSocket Test (Without Postman)

If Postman still doesn't work, try browser JavaScript:

```html
<!DOCTYPE html>
<html>
<head>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js"></script>
</head>
<body>
    <h1>WebSocket Test</h1>
    <button onclick="connect()">Connect</button>
    <button onclick="sendMessage()">Send Message</button>
    <div id="messages"></div>

    <script>
        let stompClient = null;
        const userId = '62b08293-e714-45e1-9bec-a4a7e9e1bc71';
        
        function connect() {
            const socket = new SockJS('https://localweb.phuongy.works/ws');
            stompClient = Stomp.over(socket);
            
            stompClient.connect(
                {'Authorization': 'Bearer ' + userId},
                function(frame) {
                    console.log('Connected: ' + frame);
                    
                    // Subscribe to messages
                    stompClient.subscribe('/user/' + userId + '/queue/messages', function(message) {
                        console.log('Received:', message.body);
                        document.getElementById('messages').innerHTML += 
                            '<p>' + message.body + '</p>';
                    });
                },
                function(error) {
                    console.error('Error:', error);
                }
            );
        }
        
        function sendMessage() {
            stompClient.send('/app/chat.send', {}, JSON.stringify({
                'content': 'Test from browser',
                'recipientId': '659235bc-60c6-45b3-bab8-2adb83b0892e'
            }));
        }
    </script>
</body>
</html>
```

Save as `websocket-test.html` and open in browser.

---

### Step 10: Backend Code Verification

Check if `ChatController.sendMessage()` is actually being called:

```java
@MessageMapping("/chat.send")
public void sendMessage(@Payload ChatMessagePayload payload, Principal principal) {
    if (principal == null) {
        log.error("❌ No principal - user not authenticated");
        return;
    }
    
    String senderId = principal.getName();
    log.info("✅ Message received from: {} to: {}", senderId, payload.getRecipientId());
    
    // ... rest of method
}
```

**If principal is null:**
- WebSocket authentication interceptor not working
- User ID not being set in CONNECT frame
- Authorization header not being processed

---

## Quick Fix Checklist

Run through this checklist:

- [ ] WebSocket URL is `wss://localweb.phuongy.works/ws/websocket` ✅
- [ ] CONNECT frame has `Authorization:Bearer <USER_ID>` ✅
- [ ] Subscription destination is `/user/<USER_ID>/queue/messages` ✅
- [ ] SEND destination is `/app/chat.send` ✅
- [ ] JSON payload includes `recipientId` ✅
- [ ] NULL byte (`^@`) is added after each frame ✅
- [ ] Backend logs show message received ❓ (need to check)
- [ ] Backend logs show "Sending to RECIPIENT" ❓ (need to check)
- [ ] Backend logs show "Sending to SENDER" ❓ (need to check)
- [ ] No errors in backend logs ❓ (need to check)
- [ ] Kafka is running and consumers are active ❓ (need to check)

---

## Next Steps

1. **Check backend logs** - This is the most important step!
   ```bash
   docker-compose logs -f communication-service | grep -E "(📥|📤|✅|❌)"
   ```

2. **Enable debug logging** - Add to `application.yaml`:
   ```yaml
   logging:
     level:
       org.springframework.messaging: DEBUG
       org.springframework.web.socket: DEBUG
   ```

3. **Test with browser** - Use the HTML test page above

4. **Check Kafka** - Ensure Kafka consumer is processing messages:
   ```bash
   docker-compose logs -f kafka
   docker-compose logs -f communication-service | grep Kafka
   ```

5. **Verify database** - Check if messages are being saved:
   ```sql
   SELECT * FROM messages ORDER BY sent_at DESC LIMIT 10;
   ```

---

## Common Log Messages & What They Mean

### ✅ Success Messages:
```
📥 Tin nhắn nhận được: Từ X -> Tới Y
  → Message received by ChatController

✅ Message saved to database with status SENT. MessageId: <uuid>
  → Message persisted successfully

📤 Publishing message to Kafka topic: chat-messages
  → Message queued for Kafka

📤 Sending message to RECIPIENT: userId=Y
  → Attempting to send to recipient via WebSocket

📤 Sending message to SENDER: userId=X
  → Attempting to send confirmation to sender via WebSocket

✅ Message <uuid> sent to both users
  → WebSocket send completed
```

### ❌ Error Messages:
```
❌ Gửi tin nhắn thất bại. Không tìm thấy principal
  → Authentication failed - no user ID in session

❌ Error consuming chat message from Kafka
  → Kafka consumer error

❌ Failed to publish message to Kafka
  → Kafka producer error

❌ Error sending message via WebSocket
  → WebSocket connection issue or user not subscribed
```

---

## Still Not Working?

If you've tried everything above and still not receiving messages, please provide:

1. **Backend logs** when sending a message (full output)
2. **Postman response panel** - what do you see after sending?
3. **Postman message list** - do you see any MESSAGE frames at all?
4. **Docker status**: `docker-compose ps`
5. **Kafka status**: `docker-compose logs kafka | tail -50`

---

**Last Updated:** 2025-11-11
**Version:** 1.0
