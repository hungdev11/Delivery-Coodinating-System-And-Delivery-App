**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# API Documentation: Parcel Service Controller (v2)

**Base Path:** `/api/v2/parcels`

This controller acts as a proxy to the `parcel-service` for listing parcels with enhanced filtering. All routes require authentication.

| Method | Path | Business Function                                   | Java Method Name | Roles Allowed      | Proxied To                           |
|--------|------|-----------------------------------------------------|------------------|--------------------|--------------------------------------|
| `POST` | `/`  | List parcels based on a query (enhanced filtering). | `listParcels`    | Authenticated User | `parcel-service` (`/api/v2/parcels`) |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)