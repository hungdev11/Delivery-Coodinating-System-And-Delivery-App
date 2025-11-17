# Parcel Service API: Parcel Controller (v1)

**Base Path:** `/api/v1/parcels`

This controller provides the core CRUD, search, and state management functionality for parcels.

## Core CRUD & Search

| Method   | Path           | Business Function                              |
|----------|----------------|------------------------------------------------|
| `POST`   | `/`            | Create a new parcel.                           |
| `PUT`    | `/{parcelId}`  | Update a parcel.                               |
| `GET`    | `/{parcelId}`  | Get a parcel by its ID.                        |
| `GET`    | `/code/{code}` | Get a parcel by its code.                      |
| `GET`    | `/me`          | Get parcels sent by the current user.          |
| `GET`    | `/me/receive`  | Get parcels to be received by the current user.|
| `GET`    | `/`            | Get parcels with filtering, sorting, and pagination. |
| `DELETE` | `/{parcelId}`  | Delete a parcel.                               |
| `POST`   | `/bulk`        | Fetch a bulk list of parcels by their IDs.     |

## State Management

| Method | Path                        | Business Function                         | Corresponding Event         |
|--------|-----------------------------|-------------------------------------------|-----------------------------|
| `PUT`  | `/change-status/{parcelId}` | Generic endpoint to change parcel status. | `(dynamic)`                 |
| `PUT`  | `/deliver/{parcelId}`       | Mark parcel as delivered.                 | `DELIVERY_SUCCESSFUL`       |
| `PUT`  | `/confirm/{parcelId}`       | Customer confirms receipt.                | `CUSTOMER_RECEIVED`         |
| `PUT`  | `/accident/{parcelId}`      | Shipper reports an incident.              | `CAN_NOT_DELIVERY`          |
| `PUT`  | `/refuse/{parcelId}`        | Customer refuses the parcel.              | `CUSTOMER_REJECT`           |
| `PUT`  | `/postpone/{parcelId}`      | Shipper postpones delivery.               | `POSTPONE`                  |

## Dispute Management

| Method | Path                                     | Business Function                             | Corresponding Event                |
|--------|------------------------------------------|-----------------------------------------------|------------------------------------|
| `PUT`  | `/dispute/{parcelId}`                    | Customer opens a dispute (not received).      | `CUSTOMER_CONFIRM_NOT_RECEIVED`    |
| `PUT`  | `/resolve-dispute/misunderstanding/{parcelId}` | Admin resolves dispute (misunderstanding).    | `MISSUNDERSTANDING_DISPUTE`        |
| `PUT`  | `/resolve-dispute/fault/{parcelId}`      | Admin resolves dispute (shipper fault/lost). | `FAULT_DISPUTE`                    |

## Other Operations

| Method | Path                   | Business Function                      |
|--------|------------------------|----------------------------------------|
| `PUT`  | `/{parcelId}/priority` | Update the priority of a parcel.       |
| `PUT`  | `/{parcelId}/delay`    | Delay/postpone a parcel's delivery.    |
| `PUT`  | `/{parcelId}/undelay`  | Resume a delayed parcel.               |
