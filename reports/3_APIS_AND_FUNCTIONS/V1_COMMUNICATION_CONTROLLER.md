# API Documentation: Communication Controller (v1)

**Base Path:** `/api/v1`

This controller acts as a proxy to the `communication-service`, handling conversations, messages, and proposals. It also enriches some responses with additional data from other services. All routes require authentication.

## Conversation Endpoints

| Method | Path                                     | Business Function                                                    | Java Method Name                 | Notes                                                 |
|--------|------------------------------------------|----------------------------------------------------------------------|----------------------------------|-------------------------------------------------------|
| `GET`  | `/conversations/user/{userId}`           | Get conversations for a user (direct proxy).                         | `getMyConversations`             | Proxies to `communication-service`.                   |
| `GET`  | `/conversations`                         | Get enriched conversations for the Android client.                   | `getEnrichedConversations`       | Enriches response with user, parcel, and session info.|
| `GET`  | `/conversations/find-by-users`           | Find or create a conversation between two users, with enriched data. | `getConversationByTwoUsers`      | Enriches response with partner name.                  |
| `POST` | `/conversations/find-by-users`           | (Alternative to GET) Find or create a conversation.                  | `getConversationByTwoUsersUsePost` | Proxies to `communication-service`.                   |
| `GET`  | `/conversations/{conversationId}/messages` | Get messages for a conversation.                                     | `getMessages`                    | Proxies to `communication-service`.                   |

## Proposal Endpoints

| Method | Path                              | Business Function                         | Java Method Name        | Notes                         |
|--------|-----------------------------------|-------------------------------------------|-------------------------|-------------------------------|
| `POST` | `/proposals`                      | Create a new proposal.                    | `createProposal`        | Proxies to `communication-service`. |
| `POST` | `/proposals/{proposalId}/respond` | Respond to a proposal.                    | `respondToProposal`     | Proxies to `communication-service`. |
| `GET`  | `/proposals/available-configs`    | Get available proposal configs for roles. | `getAvailableConfigs`   | Proxies to `communication-service`. |

## Admin Proposal Config Endpoints

These endpoints are intended for admin users.

| Method   | Path                                   | Business Function            | Java Method Name         | Notes                         |
|----------|----------------------------------------|------------------------------|--------------------------|-------------------------------|
| `GET`    | `/admin/proposals/configs`             | Get all proposal configs.    | `getAllProposalConfigs`  | Proxies to `communication-service`. |
| `POST`   | `/admin/proposals/configs`             | Create a proposal config.    | `createProposalConfig`   | Proxies to `communication-service`. |
| `PUT`    | `/admin/proposals/configs/{configId}`  | Update a proposal config.    | `updateProposalConfig`   | Proxies to `communication-service`. |
| `DELETE` | `/admin/proposals/configs/{configId}`  | Delete a proposal config.    | `deleteProposalConfig`   | Proxies to `communication-service`. |

## Message Endpoints

| Method | Path                         | Business Function                       | Java Method Name      | Notes                         |
|--------|------------------------------|-----------------------------------------|-----------------------|-------------------------------|
| `POST` | `/messages`                  | Send a new message.                     | `sendMessage`         | Proxies to `communication-service`. |
| `PUT`  | `/messages/{messageId}/status` | Update message status (e.g., to "READ"). | `updateMessageStatus` | Proxies to `communication-service`. |
