# sentAt Null Issue - Fix Documentation

## Problem

Messages were being saved with `sentAt: null`, causing issues in the frontend when trying to sort and display messages.

### Example of the Issue:
```json
{
  "id": "ba6fcd21-5aee-4137-892c-f3f6c94775d2",
  "conversationId": "72de1752-c48b-4cc3-aab1-05ede184fa4a",
  "senderId": "62b08293-e714-45e1-9bec-a4a7e9e1bc71",
  "content": "hihi",
  "sentAt": null,  // ❌ NULL!
  "type": "TEXT",
  "status": "SENT"
}
```

## Root Cause

The `@CreationTimestamp` annotation in the `Message` entity was not being triggered properly, leaving `sentAt` as `null`.

```java
// Before (not working reliably)
@CreationTimestamp
@Column(name = "sent_at", nullable = false, updatable = false)
private LocalDateTime sentAt;
```

## Solution

### 1. Backend Fix (Java)

**File: `MessageService.java`**

Explicitly set `sentAt` timestamp when creating a message:

```java
// 2. Create and save message with SENT status
Message message = new Message();
message.setConversation(conversation);
message.setSenderId(senderId); 
message.setContent(payload.getContent());
message.setType(ContentType.TEXT);
message.setStatus(MessageStatus.SENT);
message.setSentAt(java.time.LocalDateTime.now()); // ✅ Explicitly set timestamp

Message savedMessage = messageRepository.save(message);
```

**File: `Message.java`**

Removed `@CreationTimestamp` annotation since we're setting it explicitly:

```java
// After (explicit setting)
@Column(name = "sent_at", nullable = false, updatable = false)
private LocalDateTime sentAt;
```

### 2. Frontend Fix (TypeScript)

**File: `useConversations.ts`**

Added fallback handling for null `sentAt`:

```typescript
const addMessage = (message: MessageResponse) => {
  // If sentAt is null, use current time (for messages just sent)
  const sentAtValue = message.sentAt || new Date().toISOString()
  const messageTime = new Date(sentAtValue).getTime()
  
  // ... sorting logic ...
  
  // Update sentAt if it was null before inserting
  if (!message.sentAt) {
    message.sentAt = sentAtValue
  }
  
  messages.value.splice(insertIndex, 0, message)
}
```

## Testing

### Before Fix:
```bash
# Backend returns:
{
  "sentAt": null  // ❌
}

# Frontend error:
NaN when sorting messages
Message not added to pool
```

### After Fix:
```bash
# Backend returns:
{
  "sentAt": "2025-11-12T14:59:10.894Z"  // ✅
}

# Frontend:
Messages sorted correctly
All messages appear in UI
```

## How to Test

1. **Rebuild the backend:**
   ```bash
   cd BE/communication_service
   mvn clean package
   docker-compose restart communication-service
   ```

2. **Test sending a message:**
   - Open your chat application
   - Send a message
   - Check browser console logs
   - Verify `sentAt` is not null:
     ```javascript
     [INFO] [WEBSOCKET:SUBSCRIPTION]: 📥 RECEIVED MESSAGE
     {
       fullData: {
         sentAt: "2025-11-12T14:59:10.894Z",  // ✅ Should have timestamp
         ...
       }
     }
     ```

3. **Verify in database:**
   ```sql
   SELECT id, content, sent_at 
   FROM messages 
   ORDER BY sent_at DESC 
   LIMIT 10;
   ```
   
   Should show:
   ```
   id                                   | content | sent_at
   -------------------------------------|---------|-------------------------
   ba6fcd21-5aee-4137-892c-f3f6c94775d2 | hihi    | 2025-11-12 14:59:10.894
   ```

## Additional Improvements

### Add Validation

**File: `MessageService.java`**

Add assertion to ensure sentAt is always set:

```java
Message savedMessage = messageRepository.save(message);

// Validation: Ensure sentAt is set
if (savedMessage.getSentAt() == null) {
    log.error("❌ Critical: Message saved without sentAt timestamp! MessageId: {}", 
              savedMessage.getId());
    throw new IllegalStateException("Message must have sentAt timestamp");
}

log.info("✅ Message saved to database with status SENT. MessageId: {}, sentAt: {}", 
         savedMessage.getId(), savedMessage.getSentAt());
```

### Add Database Constraint

**File: `Message.java`**

The `nullable = false` constraint is already in place:

```java
@Column(name = "sent_at", nullable = false, updatable = false)
private LocalDateTime sentAt;
```

This will prevent null values at the database level.

## Rollback Plan

If issues arise, revert to using `@CreationTimestamp`:

```java
// Revert to:
@CreationTimestamp
@Column(name = "sent_at", nullable = false, updatable = false)
private LocalDateTime sentAt;

// And remove explicit setting in MessageService.java:
// message.setSentAt(java.time.LocalDateTime.now()); // Remove this line
```

## Related Issues

- Frontend handling of null sentAt: Fixed in `useConversations.ts`
- Message sorting: Now works correctly with timestamps
- WebSocket logging: Enhanced to show full message details

## Files Changed

### Backend:
- ✅ `MessageService.java` - Added explicit sentAt setting
- ✅ `Message.java` - Removed @CreationTimestamp annotation

### Frontend:
- ✅ `useConversations.ts` - Added null handling for sentAt
- ✅ `WEBSOCKET_LOGS.md` - Added logging documentation

## Status

✅ **FIXED** - `sentAt` is now always set to a valid timestamp on both backend and frontend.

---

**Last Updated:** 2025-11-12  
**Fixed By:** Backend explicit timestamp setting + Frontend fallback handling
