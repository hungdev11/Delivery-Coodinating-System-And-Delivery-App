**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# API Documentation: Parcel Service Controller (v1)

**Base Path:** `/api/v1/parcels`

This controller acts as a proxy to the `parcel-service` for all parcel-related operations.

| Method   | Path                      | Business Function                          | Java Method Name     | Proxied To       |
|----------|---------------------------|--------------------------------------------|----------------------|------------------|
| `POST`   | `/`                       | Create a new parcel.                       | `createParcel`       | `parcel-service` |
| `PUT`    | `/{parcelId}`             | Update a parcel.                           | `updateParcel`       | `parcel-service` |
| `GET`    | `/{parcelId}`             | Get a parcel by its ID.                    | `getParcelById`      | `parcel-service` |
| `GET`    | `/code/{code}`            | Get a parcel by its code.                  | `getParcelByCode`    | `parcel-service` |
| `GET`    | `/me`                     | Get parcels sent by a customer.            | `getParcelsSent`     | `parcel-service` |
| `GET`    | `/me/receive`             | Get parcels to be received by a customer.  | `getParcelsReceive`  | `parcel-service` |
| `PUT`    | `/change-status/{parcelId}` | Change the status of a parcel.             | `changeParcelStatus` | `parcel-service` |
| `DELETE` | `/{parcelId}`             | Delete a parcel.                           | `deleteParcel`       | `parcel-service` |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)