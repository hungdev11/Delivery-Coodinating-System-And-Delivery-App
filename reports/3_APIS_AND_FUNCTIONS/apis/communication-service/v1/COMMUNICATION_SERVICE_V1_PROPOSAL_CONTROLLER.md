**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# Communication Service API: Proposal Controller (v1)

**Base Path:** `/api/v1/proposals`

This controller, part of the `communication-service`, manages interactive proposals between users.

| Method | Path                   | Business Function                                            | Java Method Name      |
|--------|------------------------|--------------------------------------------------------------|-----------------------|
| `POST` | `/`                    | Create a new proposal (e.g., a shipper's cancellation request). | `createProposal`      |
| `POST` | `/{proposalId}/respond`| Respond to a proposal.                                       | `respondToProposal`   |
| `GET`  | `/available-configs`   | Get available proposal configurations based on user roles.   | `getAvailableConfigs` |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)