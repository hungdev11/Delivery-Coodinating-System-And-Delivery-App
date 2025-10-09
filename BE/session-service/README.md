# SESSION SERVICE 
- Port: 21505
- DB: ds_session_service
# 📘 Shift API

**Base URL:** `/api/shifts`

| HTTP Method | Endpoint | Description | Request Body | Response |
|--------------|-----------|--------------|----------------|------------|
| **POST** | `/api/shifts` | Tạo mới ca làm việc | `ShiftRequest` | `Shift` |
| **PUT** | `/api/shifts/{id}` | Cập nhật thông tin ca làm việc | `ShiftRequest` | `Shift` |
| **GET** | `/api/shifts` | Lấy danh sách tất cả ca làm việc | — | `List<Shift>` |
| **GET** | `/api/shifts/{id}` | Lấy thông tin chi tiết ca làm việc theo ID | — | `Shift` |
| **DELETE** | `/api/shifts/{id}` | Xóa ca làm việc | — | `204 No Content` |
| **GET** | `/api/shifts/type/{type}` | Lấy ca làm việc theo loại (enum `ShiftType`) | — | `Shift` |
| **GET** | `/api/shifts/validate?type={type}&start={HH:mm}&end={HH:mm}` | Kiểm tra khung giờ có nằm trong ca làm việc không | — | `Boolean` |

---

# 📦 Delivery Shift API

**Base URL:** `/api/delivery-shifts`

| HTTP Method | Endpoint | Description | Request Body | Response |
|--------------|-----------|--------------|----------------|------------|
| **POST** | `/api/delivery-shifts/assign` | Gán một ca cho shipper | `{ "deliveryManId": "string", "shiftId": number }` | `DeliveryManShift` |
| **POST** | `/api/delivery-shifts/bulk-assign` | Gán cùng một ca cho nhiều shipper | `{ "shipperIds": ["string"], "shiftId": number }` | `List<DeliveryManShift>` |
| **DELETE** | `/api/delivery-shifts/{deliveryManShiftId}` | Hủy ca làm việc của shipper | — | `204 No Content` |
| **GET** | `/api/delivery-shifts/by-shift/{shiftId}` | Lấy danh sách shipper trong một ca làm việc | — | `List<DeliveryManShift>` |
| **GET** | `/api/delivery-shifts/by-shipper/{shipperId}` | Lấy danh sách ca làm việc của một shipper | — | `List<DeliveryManShift>` |

---

## 🧩 Models

### `ShiftRequest`
```json
{
  "type": "MORNING", 
  "startTime": "08:00:00", 
  "endTime": "12:00:00"
}
```

### `Shift`
```json
{
  "id": 1,
  "type": "MORNING",
  "startTime": "08:00:00", 
  "endTime": "12:00:00"
}
```

### `DeliveryManShift`
```json
{
  "id": "de0bcb3e-5633-47de-a1c1-96ec9a61c941",
  "deliveryManId": UUID,
  "shiftId": 1,
  "isActive": true
}
```