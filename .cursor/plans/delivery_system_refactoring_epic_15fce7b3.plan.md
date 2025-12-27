---
name: Delivery System Refactoring Epic
overview: "Refactor hệ thống giao hàng từ mô hình tự quét QR sang mô hình admin quản lý task assignment với zone-based routing và auto-assignment sử dụng OSRM + JSPRIT. Bao gồm: Parcel model refactoring (dùng address IDs), DeliveryMan extensions (WorkingZones, WorkingShift), DeliveryAssignment refactoring (quan hệ 1-n với parcels, thêm status PENDING), Admin manual/auto assignment, Ticket system, và seed data updates."
todos: []
---

# Delivery System Refactoring Epic

## Overview

Epic này refactor toàn bộ hệ thống giao hàng từ mô hình hiện tại (shipper tự quét QR tạo assignment) sang mô hình mới (admin quản lý task assignment với zone-based routing và auto-assignment).

## Architecture Changes

### Data Model Changes

**Parcel Model:**

- Bỏ `receiveFrom`, `sendTo` (string addresses)
- Thêm `senderAddressId`, `receiverAddressId` (UUID references to UserAddress)
- DeliveryType enum map sang priority: ECONOMY(0-1), NORMAL(2-4), FAST(5-6), EXPRESS(7-9), URGENT(10)
- Priority field được tính tự động từ DeliveryType

**DeliveryMan Extensions:**

- `WorkingZone` entity: 1-n với DeliveryMan, max 5, có `order` field (thứ tự ưu tiên)
- `WorkingShift` entity: quản lý ca làm việc (mặc định 8h-18h, T2-T6)
- `LeaveRequest` entity: đăng ký nghỉ (shiftId, startTime, endTime)

**DeliveryAssignment Refactoring:**

- Thêm status `PENDING` (chờ shipper nhận)
- Status flow: `PENDING` → `ACCEPTED` → `IN_PROGRESS` → `COMPLETED`/`FAILED`
- Bảng trung gian `DeliveryAssignmentParcel`: quan hệ 1-n với Parcels
- Lưu `deliveryAddressId` (vì các parcel trong cùng assignment phải chung 1 địa chỉ giao)
- Bỏ logic shipper tự quét QR tạo assignment

**DeliverySession:**

- Chỉ admin tạo session và gán assignment
- Shipper xác nhận nhận task (dùng QR code để xác nhận)
- Khi bắt đầu session, tất cả parcels → status `ON_ROUTE`

### New Services

**Auto Assignment Service (session-service):**

- Integration với OSRM `/table/v1/driving/` API từ zone_service
- Sử dụng JSPRIT để giải bài toán VRP với constraints:
- Workload balancing (fairness constraint)
- Session time limits (maxSessionTime: 3.5h sáng, 4.5h chiều)
- Zone-based filtering (theo WorkingZones order)
- P0 parcels: tối đa 3 đơn, tối ưu số shipper

**Ticket System (communication-service):**

- Ticket entity: link với Parcel, DeliveryAssignment, User (client/shipper)
- Types: DELIVERY_FAILED (shipper báo), NOT_RECEIVED (client báo)
- Admin có thể: reassign parcel, cancel parcel, mark as resolved

## Stories

### Story 1: Parcel Model Refactoring & DeliveryType Priority Mapping

**Scope:**

- Refactor Parcel entity: bỏ `receiveFrom`/`sendTo`, thêm `senderAddressId`/`receiverAddressId`
- Thêm utility method map DeliveryType → priority
- Update Parcel DTOs và API endpoints
- Migration script cho existing data (nếu có)

**Files:**

- `BE/parcel-service/src/main/java/com/ds/parcel_service/app_context/models/Parcel.java`
- `BE/parcel-service/src/main/java/com/ds/parcel_service/common/enums/DeliveryType.java` (thêm priority mapping)
- Parcel DTOs và controllers
- Migration scripts

**Acceptance Criteria:**

- Parcel chỉ lưu address IDs, không lưu address strings
- DeliveryType map đúng priority (ECONOMY:0-1, NORMAL:2-4, FAST:5-6, EXPRESS:7-9, URGENT:10)
- API create/update parcel yêu cầu address IDs

---

### Story 2: DeliveryMan Extensions (WorkingZones, WorkingShift, LeaveRequest)

**Scope:**

- Tạo `WorkingZone` entity (1-n với DeliveryMan, max 5, có order)
- Tạo `WorkingShift` entity (quản lý ca làm: default 8h-18h, T2-T6)
- Tạo `LeaveRequest` entity (đăng ký nghỉ: shiftId, startTime, endTime)
- Repository và basic CRUD services
- Seed data trong application.yaml cho shippers (thêm WorkingZones và shifts)

**Files:**

- `BE/User_service/src/main/java/com/ds/user/common/entities/base/WorkingZone.java` (new)
- `BE/User_service/src/main/java/com/ds/user/common/entities/base/WorkingShift.java` (new)
- `BE/User_service/src/main/java/com/ds/user/common/entities/base/LeaveRequest.java` (new)
- `BE/User_service/src/main/java/com/ds/user/app_context/repositories/WorkingZoneRepository.java` (new)
- `BE/User_service/src/main/java/com/ds/user/app_context/repositories/WorkingShiftRepository.java` (new)
- `BE/User_service/src/main/java/com/ds/user/app_context/repositories/LeaveRequestRepository.java` (new)
- `BE/User_service/src/main/resources/application.yaml` (update seed data)

**Acceptance Criteria:**

- DeliveryMan có thể có tối đa 5 WorkingZones với order field
- WorkingShift có default schedule (8h-18h, T2-T6)
- LeaveRequest có thể được tạo và query được
- Seed data có WorkingZones và shifts cho các shippers

---

### Story 3: DeliveryAssignment Refactoring (1-n Parcels, PENDING Status)

**Scope:**

- Thêm status `PENDING` vào AssignmentStatus enum
- Tạo `DeliveryAssignmentParcel` junction table
- Thêm `deliveryAddressId` vào DeliveryAssignment (vì parcels cùng 1 địa chỉ giao)
- Update DeliveryAssignment entity và methods
- Update status transition logic: PENDING → ACCEPTED → IN_PROGRESS → COMPLETED/FAILED

**Files:**

- `BE/session-service/src/main/java/com/ds/session/session_service/common/enums/AssignmentStatus.java`
- `BE/session-service/src/main/java/com/ds/session/session_service/app_context/models/DeliveryAssignment.java`
- `BE/session-service/src/main/java/com/ds/session/session_service/app_context/models/DeliveryAssignmentParcel.java` (new)
- `BE/session-service/src/main/java/com/ds/session/session_service/app_context/repositories/DeliveryAssignmentParcelRepository.java` (new)
- DeliveryAssignmentService updates

**Acceptance Criteria:**

- Assignment có status PENDING khi được admin tạo
- Assignment có quan hệ 1-n với Parcels qua junction table
- Assignment lưu deliveryAddressId (địa chỉ giao chung cho tất cả parcels)
- Status transitions đúng flow

---

### Story 4: Zone-Based Parcel Filtering Service

**Scope:**

- Service để filter parcels theo WorkingZones của shipper (theo order)
- Integration với zone_service để query zone từ coordinates
- API endpoint để lấy danh sách parcels đã filter theo zone (cho admin manual assignment)

**Files:**

- `BE/session-service/src/main/java/com/ds/session/session_service/business/v1/services/ZoneFilteringService.java` (new)
- Controller endpoints cho manual assignment filtering
- Integration với zone_service (coordinate → zone lookup)

**Acceptance Criteria:**

- Có thể filter parcels theo WorkingZones của shipper (ưu tiên theo order)
- API trả về danh sách parcels đã filter và sắp xếp theo zone priority
- Hỗ trợ fallback khi zone ưu tiên hết parcels thì chuyển sang zone tiếp theo

---

### Story 5: OSRM Integration Service (Matrix API)

**Scope:**

- Service gọi OSRM `/table/v1/driving/` API từ zone_service
- Parse response và convert sang JSPRIT `VehicleRoutingTransportCostsMatrix`
- Handle errors và retries
- Caching nếu cần

**Files:**

- `BE/session-service/src/main/java/com/ds/session/session_service/infrastructure/osrm/OSRMService.java` (new)
- `BE/session-service/src/main/java/com/ds/session/session_service/infrastructure/osrm/OSRMMatrixResponse.java` (new DTOs)
- Configuration cho OSRM endpoint

**Acceptance Criteria:**

- Có thể gọi OSRM table API và parse response
- Trả về JSPRIT-compatible matrix
- Handle errors gracefully

---

### Story 6: Auto Assignment Service (JSPRIT VRP Solver)

**Scope:**

- Implement JSPRIT VRP solver theo spec trong autp-tasks.json
- Input: Shippers (với start location, shift times, maxSessionTime, capacity), Orders (Parcels với coordinates, serviceTime, priority)
- Constraints: Workload balancing, session time limits, zone-based, P0 parcels (max 3, optimize shipper count)
- Output: Map<String, List<Task>> với order_id, sequence_index, estimated_arrival_time, travel_time_from_previous_stop

**Files:**

- `BE/session-service/src/main/java/com/ds/session/session_service/business/v1/services/AutoAssignmentService.java` (new)
- `BE/session-service/src/main/java/com/ds/session/session_service/business/v1/services/RoutingService.java` (new, wraps JSPRIT)
- Model classes cho Order, Shipper, Task
- Dependencies: jsprit-core

**Acceptance Criteria:**

- Giải được bài toán VRP với workload balancing
- Tôn trọng session time limits (3.5h/4.5h)
- P0 parcels được ưu tiên (tối đa 3, tối ưu shipper count)
- Output format đúng spec

---

### Story 7: Admin Manual Task Assignment API

**Scope:**

- API endpoint để admin tạo DeliveryAssignment manually
- Filter parcels theo zone (sử dụng ZoneFilteringService)
- Validation: parcels phải cùng delivery address, shipper có WorkingZone phù hợp
- Create assignment với status PENDING

**Files:**

- `BE/session-service/src/main/java/com/ds/session/session_service/application/controllers/v1/AdminAssignmentController.java` (new)
- `BE/session-service/src/main/java/com/ds/session/session_service/business/v1/services/AdminAssignmentService.java` (new)
- DTOs cho manual assignment requests

**Acceptance Criteria:**

- Admin có thể tạo assignment manually
- Parcels được filter theo WorkingZones
- Validation đúng (cùng delivery address, zone match)
- Assignment được tạo với status PENDING

---

### Story 8: Admin Auto Task Assignment API

**Scope:**

- API endpoint để admin trigger auto assignment
- Sử dụng AutoAssignmentService để tính toán
- Create assignments và DeliveryAssignmentParcel records
- Return assignment results

**Files:**

- Extend `AdminAssignmentController` với auto assignment endpoint
- `AdminAssignmentService` integration với AutoAssignmentService
- DTOs cho auto assignment requests/responses

**Acceptance Criteria:**

- Admin có thể trigger auto assignment
- Auto assignment tạo assignments với parcels phù hợp
- Tôn trọng constraints (workload, time, zone, P0)
- Return results với task details

---

### Story 9: DeliverySession Admin Creation & Shipper Workflow

**Scope:**

- Admin tạo DeliverySession và gán assignments
- Shipper xác nhận nhận task (dùng QR code - assignment ID)
- Shipper bắt đầu session → tất cả parcels → ON_ROUTE, assignments → IN_PROGRESS
- Update SessionService và AssignmentService

**Files:**

- `BE/session-service/src/main/java/com/ds/session/session_service/business/v1/services/SessionService.java` (update)
- `BE/session-service/src/main/java/com/ds/session/session_service/business/v1/services/DeliveryAssignmentService.java` (update)
- Controller endpoints cho shipper workflow

**Acceptance Criteria:**

- Admin tạo session và gán assignments
- Shipper có thể xác nhận nhận task (QR scan)
- Khi bắt đầu session, parcels → ON_ROUTE, assignments → IN_PROGRESS
- Workflow đúng như spec

---

### Story 10: Ticket System (Communication Service)

**Scope:**

- Tạo Ticket entity trong communication-service
- Types: DELIVERY_FAILED (shipper báo), NOT_RECEIVED (client báo)
- Link với Parcel, DeliveryAssignment, User (client/shipper)
- Admin actions: reassign parcel, cancel parcel, mark resolved
- API endpoints để tạo và quản lý tickets

**Files:**

- `BE/communication_service/src/main/java/com/ds/communication_service/app_context/models/Ticket.java` (new)
- `BE/communication_service/src/main/java/com/ds/communication_service/app_context/repositories/TicketRepository.java` (new)
- `BE/communication_service/src/main/java/com/ds/communication_service/business/v1/services/TicketService.java` (new)
- `BE/communication_service/src/main/java/com/ds/communication_service/application/controllers/v1/TicketController.java` (new)
- DTOs và enums

**Acceptance Criteria:**

- Có thể tạo ticket khi shipper báo delivery failed
- Có thể tạo ticket khi client báo not received
- Admin có thể xem tickets và link với parcels/assignments
- Admin có thể reassign/cancel/resolve tickets

---

### Story 11: Parcel Seed Data Updates

**Scope:**

- Update seed data: 30 clients, mỗi client 1-3 addresses (tổng ~70 addresses)
- Tất cả addresses trong khu vực thuduc_cu.poly
- Update ParcelSeedService để dùng address IDs thay vì address strings
- Shippers có WorkingZones và WorkingShifts trong seed data

**Files:**

- `BE/User_service/src/main/resources/application.yaml` (update seed data)
- `BE/User_service/src/main/java/com/ds/user/application/startup/data/services/ParcelSeedService.java` (update logic)

**Acceptance Criteria:**

- Seed data có 30 clients với ~70 addresses
- Tất cả addresses trong thuduc_cu.poly bounds
- ParcelSeedService dùng address IDs
- Shippers có WorkingZones và shifts

---

### Story 12: Management System UI - Task Assignment

**Scope:**

- UI cho admin manual assignment: filter parcels theo zone, select parcels, assign to shipper
- UI cho admin auto assignment: trigger và xem results
- UI để xem danh sách assignments và sessions
- UI để quản lý tickets

**Files:**

- `ManagementSystem/src/` components và pages mới
- API integration với session-service và communication-service

**Acceptance Criteria:**

- Admin có thể manual assign tasks
- Admin có thể trigger auto assignment
- Admin có thể xem và quản lý assignments/sessions
- Admin có thể quản lý tickets

---

### Story 13: API Gateway Updates

**Scope:**

- Expose các API mới từ session-service (admin assignment, auto assignment)
- Expose ticket APIs từ communication-service
- Update routing và authentication

**Files:**

- `BE/api-gateway/src/main/java/com/ds/gateway/` controllers và clients

**Acceptance Criteria:**

- Tất cả APIs mới được expose qua gateway
- Authentication và authorization đúng

---

### Story 14: Integration Testing & Documentation

**Scope:**

- Integration tests cho các flows mới
- Update API documentation
- Migration guide nếu cần

**Files:**

- Test files
- Documentation files

**Acceptance Criteria:**
-