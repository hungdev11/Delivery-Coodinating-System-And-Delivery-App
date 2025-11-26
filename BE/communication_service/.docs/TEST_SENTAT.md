# Testing sentAt Timestamp Fix

## Quick Test

### 1. Rebuild & Restart Backend

```bash
cd BE/communication_service

# Build
mvn clean package -DskipTests

# If using Docker:
docker-compose restart communication-service

# If running locally:
# Stop the service, then:
java -jar target/communication-service-*.jar
```

### 2. Test via Frontend

1. Open your chat application
2. Send a message: "test sentAt fix"
3. Open browser console (F12)
4. Look for these logs:

**Expected Output:**
```javascript
// Sending:
[INFO] [WEBSOCKET:SENDER]: 📤 SENDING MESSAGE
{
  fullPayload: {
    content: "test sentAt fix",
    ...
  }
}

// Receiving:
[INFO] [WEBSOCKET:SUBSCRIPTION]: 📥 RECEIVED MESSAGE
{
  rawBody: '{"id":"...","sentAt":"2025-11-12T15:30:00.123Z",...}'  // ✅ NOT NULL
}

[INFO] [WEBSOCKET:SUBSCRIPTION]: 📨 Parsed message data
{
  fullData: {
    sentAt: "2025-11-12T15:30:00.123Z",  // ✅ HAS TIMESTAMP
    ...
  }
}
```

**If it still shows null:**
```javascript
{
  sentAt: null  // ❌ STILL NULL - Rebuild failed or code not deployed
}
```

### 3. Test via REST API

```bash
# Send message via API
curl -X POST http://localhost:8082/api/chat/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "conversationId": "YOUR_CONVERSATION_ID",
    "recipientId": "RECIPIENT_USER_ID",
    "content": "Test message"
  }'

# Expected response:
{
  "id": "uuid-here",
  "content": "Test message",
  "sentAt": "2025-11-12T15:30:00.123",  // ✅ NOT NULL
  "status": "SENT"
}
```

### 4. Check Database

```sql
-- Connect to your database
psql -U postgres -d communication_db

-- Query recent messages
SELECT 
    id,
    content,
    sent_at,
    status
FROM messages
ORDER BY sent_at DESC
LIMIT 5;

-- Expected output:
--  id                                   | content          | sent_at                  | status
-- --------------------------------------|------------------|--------------------------|-------
--  ba6fcd21-5aee-4137-892c-f3f6c94775d2 | test sentAt fix  | 2025-11-12 15:30:00.123 | SENT
```

### 5. Check Backend Logs

```bash
# View service logs
docker logs communication-service -f

# Look for:
✅ Message saved to database with status SENT. MessageId: ba6fcd21-5aee-4137-892c-f3f6c94775d2, sentAt: 2025-11-12T15:30:00.123
```

## Common Issues

### Issue 1: Still seeing null after rebuild

**Solution:**
```bash
# 1. Verify changes are in the code
cat src/main/java/com/ds/communication_service/business/v1/services/MessageService.java | grep "setSentAt"

# Should show:
message.setSentAt(java.time.LocalDateTime.now());

# 2. Force rebuild
mvn clean
rm -rf target/
mvn package -DskipTests

# 3. Restart service
docker-compose down
docker-compose up -d communication-service
```

### Issue 2: Frontend still shows null

**Solution:**
The frontend now handles null gracefully, but check:

```javascript
// In browser console:
console.log('Frontend code loaded?', typeof addMessage)

// Should show: "function"

// If shows "undefined", refresh the page hard:
Ctrl + Shift + R (Windows/Linux)
Cmd + Shift + R (Mac)
```

### Issue 3: Database constraint error

If you see:
```
ERROR: null value in column "sent_at" violates not-null constraint
```

**Solution:**
```sql
-- Check if column allows null (it shouldn't)
SELECT 
    column_name, 
    is_nullable 
FROM information_schema.columns 
WHERE table_name = 'messages' 
AND column_name = 'sent_at';

-- If is_nullable = 'YES', update schema:
ALTER TABLE messages 
ALTER COLUMN sent_at SET NOT NULL;
```

## Verification Checklist

- [ ] Backend code has `message.setSentAt(java.time.LocalDateTime.now())`
- [ ] Backend rebuilt: `mvn clean package`
- [ ] Service restarted
- [ ] Frontend console shows sentAt with timestamp (not null)
- [ ] Database shows sent_at column populated
- [ ] Backend logs show "Message saved... sentAt: [timestamp]"
- [ ] Messages appear correctly in chat UI
- [ ] Messages are sorted chronologically

## Success Criteria

✅ **All messages must have `sentAt` timestamp**
✅ **No more `sentAt: null` in responses**
✅ **Messages sort correctly in chat UI**
✅ **No console errors about NaN dates**

---

## If All Tests Pass

🎉 **SUCCESS!** The sentAt null issue is fixed!

You should see:
- ✅ Backend sets timestamp explicitly
- ✅ Database stores timestamp
- ✅ Frontend receives and displays timestamp
- ✅ Messages appear in correct order
- ✅ No null pointer errors

## If Tests Fail

Contact support with:
1. Backend logs
2. Frontend console screenshot
3. Database query results
4. Message response JSON

---

**Test Date:** ___________  
**Tested By:** ___________  
**Result:** ☐ Pass  ☐ Fail
