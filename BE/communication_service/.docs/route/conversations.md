# Conversation Routes

Base URL: `http://localhost:<port>/api/v1/conversations`

## Endpoints

### GET /conversations/:conversationId/messages
- Description: Get messages for a conversation (paginated).
- Params: `conversationId`
- Query: `userId` (required), `page` (default: 0), `size` (default: 30), `sort` (default: "sentAt,DESC")
- Response 200:
```json
{
  "content": [
    {
      "messageId": "uuid",
      "conversationId": "uuid",
      "senderId": "user-uuid",
      "recipientId": "user-uuid",
      "content": "Hello",
      "sentAt": "2025-01-15T10:30:00Z",
      "readAt": null
    }
  ],
  "page": {
    "page": 0,
    "size": 30,
    "totalElements": 50,
    "totalPages": 2
  }
}
```

### GET /conversations/find-by-users
- Description: Find or create a conversation between two users.
- Query: `user1` (required), `user2` (required)
- Response 200:
```json
{
  "conversationId": "uuid",
  "partnerId": "user-uuid",
  "partnerName": "User 1234",
  "partnerAvatar": null
}
```

### GET /conversations/user/:currentUserId
- Description: Get all conversations for a user.
- Params: `currentUserId`
- Response 200:
```json
[
  {
    "conversationId": "uuid",
    "partnerId": "user-uuid",
    "partnerName": "User 1234",
    "partnerAvatar": null
  }
]
```
