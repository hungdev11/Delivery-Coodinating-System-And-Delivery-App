**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# Communication Service API: Conversation API Controller (v1)

**Base Path:** `/api/v1/conversations`

This controller, part of the `communication-service`, handles retrieving conversations and messages.

| Method | Path                         | Business Function                               | Java Method Name            |
|--------|------------------------------|-------------------------------------------------|-----------------------------|
| `GET`  | `/{conversationId}/messages` | Get messages for a conversation with pagination. | `getMessages`               |
| `GET`  | `/find-by-users`             | Find or create a conversation between two users. | `getConversationByTwoUsers` |
| `GET`  | `/user/{currentUserId}`      | Get all conversations for a user.               | `getMyConversations`        |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)