# Postman STOMP Frame Format Guide

## ⚠️ Common Error: `Illegal header\c`

This error means your STOMP frame has **formatting issues**. STOMP is VERY strict about format!

---

## ✅ Correct STOMP Frame Format

### Rules:
1. **No extra spaces** at the end of any line
2. **Each line ends with newline** (press Enter)
3. **Blank line before NULL terminator**
4. **NULL terminator** at the end (Postman usually adds automatically)

---

## Frame 1: CONNECT

**What to type in Postman:**
```
CONNECT
Authorization:Bearer 62b08293-e714-45e1-9bec-a4a7e9e1bc71
accept-version:1.1,1.0
heart-beat:10000,10000
[ENTER]
[ENTER]
```

**Breakdown:**
- Line 1: `CONNECT` (press Enter)
- Line 2: `Authorization:Bearer <USER_ID>` (press Enter)
- Line 3: `accept-version:1.1,1.0` (press Enter)
- Line 4: `heart-beat:10000,10000` (press Enter)
- Line 5: Empty line (press Enter again)
- Postman adds NULL byte automatically

**❌ DO NOT:**
- Add spaces after colons: `Authorization: Bearer` (WRONG)
- Add spaces at end of lines: `CONNECT ` (WRONG)
- Use tabs instead of newlines
- Copy-paste with hidden characters

**✅ Correct Format:**
```
CONNECT
Authorization:Bearer 62b08293-e714-45e1-9bec-a4a7e9e1bc71
accept-version:1.1,1.0
heart-beat:10000,10000

```

---

## Frame 2: SUBSCRIBE

**What to type in Postman:**
```
SUBSCRIBE
id:sub-0
destination:/user/62b08293-e714-45e1-9bec-a4a7e9e1bc71/queue/messages
[ENTER]
[ENTER]
```

**Breakdown:**
- Line 1: `SUBSCRIBE` (press Enter)
- Line 2: `id:sub-0` (press Enter)
- Line 3: `destination:/user/<USER_ID>/queue/messages` (press Enter)
- Line 4: Empty line (press Enter again)

**✅ Correct Format:**
```
SUBSCRIBE
id:sub-0
destination:/user/62b08293-e714-45e1-9bec-a4a7e9e1bc71/queue/messages

```

**Common Mistakes:**
- ❌ Extra space: `SUBSCRIBE ` 
- ❌ Space after colon: `id: sub-0`
- ❌ No blank line before end
- ❌ Copy-pasting with hidden characters

---

## Frame 3: SEND

**What to type in Postman:**
```
SEND
destination:/app/chat.send
content-type:application/json
[ENTER]
{"content":"Hello!","recipientId":"659235bc-60c6-45b3-bab8-2adb83b0892e"}
[ENTER]
[ENTER]
```

**Breakdown:**
- Line 1: `SEND` (press Enter)
- Line 2: `destination:/app/chat.send` (press Enter)
- Line 3: `content-type:application/json` (press Enter)
- Line 4: Empty line (press Enter)
- Line 5: JSON payload (press Enter)
- Line 6: Empty line (press Enter again)

**✅ Correct Format:**
```
SEND
destination:/app/chat.send
content-type:application/json

{"content":"Hello!","recipientId":"659235bc-60c6-45b3-bab8-2adb83b0892e"}

```

**JSON Format:**
- Must be valid JSON
- No line breaks inside JSON (keep it on one line)
- Use double quotes for strings
- No trailing commas

---

## How to Type in Postman

### Step-by-Step for SUBSCRIBE Frame:

1. In Postman WebSocket, type: `SUBSCRIBE`
2. Press **Enter** (cursor moves to next line)
3. Type: `id:sub-0`
4. Press **Enter**
5. Type: `destination:/user/62b08293-e714-45e1-9bec-a4a7e9e1bc71/queue/messages`
6. Press **Enter** (cursor moves to next line)
7. Press **Enter** again (creates blank line)
8. Click **Send** button

**Result:**
```
SUBSCRIBE
id:sub-0
destination:/user/62b08293-e714-45e1-9bec-a4a7e9e1bc71/queue/messages

```

---

## Debugging Format Issues

### If you get: `Illegal header\c 'SUBSCRIBE'`

**Problem:** Extra characters or wrong format

**Solution:**
1. Clear the message box completely
2. Type each line manually (don't copy-paste)
3. Make sure to press Enter after each line
4. Add blank line at the end
5. No spaces before or after text

### If you get: `Required destination header not found`

**Problem:** Destination header missing or malformed

**Solution:**
Check your destination line:
```
destination:/user/62b08293-e714-45e1-9bec-a4a7e9e1bc71/queue/messages
```
- No spaces after `destination:`
- Must start with `/user/` or `/topic/` or `/queue/`
- User ID must match your CONNECT frame

### If you get: `No principal (user not authenticated)`

**Problem:** Authorization header not sent or wrong

**Solution:**
In CONNECT frame, ensure:
```
Authorization:Bearer <USER_ID>
```
- Use USER_ID, not JWT token
- No space after `Bearer`
- No extra spaces

---

## Complete Example (Copy Carefully)

### Tab 1 (User A)

**1. CONNECT:**
```
CONNECT
Authorization:Bearer 62b08293-e714-45e1-9bec-a4a7e9e1bc71
accept-version:1.1,1.0
heart-beat:10000,10000

```

Wait for: `CONNECTED` frame response

**2. SUBSCRIBE:**
```
SUBSCRIBE
id:sub-0
destination:/user/62b08293-e714-45e1-9bec-a4a7e9e1bc71/queue/messages

```

**3. SEND:**
```
SEND
destination:/app/chat.send
content-type:application/json

{"content":"Hi from User A","recipientId":"659235bc-60c6-45b3-bab8-2adb83b0892e"}

```

---

### Tab 2 (User B)

**1. CONNECT:**
```
CONNECT
Authorization:Bearer 659235bc-60c6-45b3-bab8-2adb83b0892e
accept-version:1.1,1.0
heart-beat:10000,10000

```

Wait for: `CONNECTED` frame response

**2. SUBSCRIBE:**
```
SUBSCRIBE
id:sub-0
destination:/user/659235bc-60c6-45b3-bab8-2adb83b0892e/queue/messages

```

**3. SEND:**
```
SEND
destination:/app/chat.send
content-type:application/json

{"content":"Hi from User B","recipientId":"62b08293-e714-45e1-9bec-a4a7e9e1bc71"}

```

---

## Expected Response

After sending a message, you should receive a **MESSAGE** frame:

```
MESSAGE
destination:/user/62b08293-e714-45e1-9bec-a4a7e9e1bc71/queue/messages
content-type:application/json
subscription:sub-0
message-id:...

{"id":"...","conversationId":"...","senderId":"62b08293-e714-45e1-9bec-a4a7e9e1bc71","content":"Hi from User A","sentAt":"...","type":"TEXT","status":"SENT","deliveredAt":null,"readAt":null,"proposal":null}
```

---

## Postman Tips

### Tip 1: Use Text Format
Make sure Postman is in "Text" mode, not "JSON" mode.

### Tip 2: Check Hidden Characters
If copy-pasting, paste into a plain text editor first to remove hidden characters.

### Tip 3: Manual Typing Works Best
For debugging, type frames manually character by character.

### Tip 4: Save Working Frames
Once you get a frame working, save it in Postman for reuse.

### Tip 5: Use Collections
Create a Postman Collection with pre-saved STOMP frames:
- Connect Frame
- Subscribe Frame
- Send Message Frame
- Disconnect Frame

---

## Testing Checklist

Before sending, verify:

- [ ] No extra spaces at end of lines
- [ ] Each header on its own line
- [ ] Blank line after headers (before body)
- [ ] Blank line after body (before NULL)
- [ ] User ID matches in Authorization and destination
- [ ] Valid JSON in message body
- [ ] Connection is established (green "Connected")

---

## Still Getting Errors?

### Error: `Illegal header\c`
→ Type frame manually, don't copy-paste

### Error: `Required destination header not found`
→ Check destination line format

### Error: `No principal`
→ Check Authorization header in CONNECT

### Error: No response after sending
→ Check backend logs: `docker-compose logs -f communication-service`

---

**Last Updated:** 2025-11-11
**Version:** 1.0
