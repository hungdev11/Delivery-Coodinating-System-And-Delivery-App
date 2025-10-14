# Parcel Service

- URL: 'http://localhost:21506/api/v1/parcels/'

## Common
### Response
- PageResponse:
```json
{
    "content": ParcelResponse[],
    "page": 0,
    "size": 10,
    "totalElements": 2,
    "totalPages": 1,
    "first": true,
    "last": true
}
```
- ParcelResponse:
```json
{
    "id": "00361eb0-73f9-4d9c-804d-68d7791e5e6c",
    "code": "PRC-A1B2C3D4",
    "senderId": "USER-S-400",
    "receiverId": "USER-R-550",
    "receiverPhoneNumber": null,
    "deliveryType": "NORMAL",
    "receiveFrom": "Toà nhà Bitexco, Bến Nghé, Quận 1, TP.HCM",
    "targetDestination": "15 Hàng Đào, Quận Hoàn Kiếm, Hà Nội",
    "status": "IN_WAREHOUSE",
    "weight": 1.15,
    "value": 250000.00,
    "createdAt": "2025-10-12T13:46:50.264459",
    "updatedAt": "2025-10-12T13:46:50.264459",
    "windowStart": null,
    "windowEnd": null,
    "deliveredAt": null
}
```
### Request
- ParcelCreateRequest:
```json
{
  "code": "PRC-BIG-VALUE",
  "senderId": "CORP-001",
  "receiverId": "PARTNER-002",
  "deliveryType": "NORMAL", 
  "receiveFrom": "Kho hàng A, Quận 7, TP.HCM",
  "sendTo": "Văn phòng B, Quận 3, TP.HCM",
  "weight": 15.5,
  "value": 500000000.00,
  "windowStart": "18:30:00",
  "windowEnd": "20:00:00"
}
```
- ParcelUpdateRequest:
```json
{
  "weight": 0.0,
  "value": 0.0
}
```

## APIs
### POST
- Description: create new parcel
- Body: ParcelCreateRequest
- Response: 201 - ParcelResponse
- Exception: 400 - Parcel code existed, ...

### GET - {:id}
- Description: get parcel by id
- Param: id
- Response: 200 - ParcelResponse
- Exception: 404 - Parcel not found

### GET - {:code}
- Description: get parcel by code
- Body: code
- Response: 200 - ParcelResponse
- Exception: 404 - Parcel not found

### GET - '?page=0&size=1&sortBy=id&direction=desc&deliveryType=FAST&status=SUCCESSED&createdFrom=2025-10-12T13:25&createdTo=2025-10-12T13:47'
- Description: get all parcels by {filter} + {pagination} 
- Body: ParcelCreateRequest
- Response: 200 - PageResponse

### PUT - change-status/{:id}?event=CUSTOMER_REJECT
- Description: change parcel status by event (define in enum)
- Response: 200 - ParcelResponse\

### DELETE