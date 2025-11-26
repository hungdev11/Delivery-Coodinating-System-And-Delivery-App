# ERD Index - Delivery System Backend

## Tổng Quan

Tài liệu này cung cấp danh sách tất cả các ERD diagrams cho từng service trong hệ thống Delivery System Backend.

## ERD Tổng Quát

📄 **[ERD_COMPLETE.md](./ERD_COMPLETE.md)** - ERD tổng quát của toàn bộ hệ thống
- Tất cả 30 bảng từ 6 services
- Tất cả quan hệ và cross-service references
- Enums và business logic tổng quan

## ERD Theo Service

### 1. User Service
📄 **[ERD_USER_SERVICE.md](./ERD_USER_SERVICE.md)** - Database: `ds_user_service`
- **3 bảng**: `users`, `delivery_mans`, `user_addresses`
- Quản lý người dùng, shipper, và địa chỉ
- Tích hợp với Keycloak

### 2. Settings Service
📄 **[ERD_SETTINGS_SERVICE.md](./ERD_SETTINGS_SERVICE.md)** - Database: `ds_settings_service`
- **1 bảng**: `system_settings`
- Quản lý cấu hình hệ thống và secrets
- Centralized configuration management

### 3. Zone Service
📄 **[ERD_ZONE_SERVICE.md](./ERD_ZONE_SERVICE.md)** - Database: `ds_zone_service`
- **14 bảng**: `centers`, `zones`, `addresses`, `roads`, `road_nodes`, `road_segments`, `destination`, `working_places`, `user_feedback`, `traffic_conditions`, `poi_priorities`, `road_overrides`, `osrm_builds`, `weight_history`, `zone_geohash_cells`
- Quản lý dữ liệu địa lý, routing, và khu vực giao hàng
- Service phức tạp nhất với nhiều bảng và quan hệ

### 4. Parcel Service
📄 **[ERD_PARCEL_SERVICE.md](./ERD_PARCEL_SERVICE.md)** - Database: `ds_parcel_service`
- **2 bảng**: `parcels`, `parcel_destinations`
- Quản lý bưu kiện và điểm đích
- Tích hợp với User Service và Zone Service

### 5. Communication Service
📄 **[ERD_COMMUNICATION_SERVICE.md](./ERD_COMMUNICATION_SERVICE.md)** - Database: `ds_communication_service`
- **5 bảng**: `conversations`, `messages`, `notifications`, `interactive_proposals`, `proposal_type_configs`
- Quản lý giao tiếp giữa người dùng
- Hỗ trợ hội thoại, tin nhắn, thông báo, và proposals

### 6. Session Service
📄 **[ERD_SESSION_SERVICE.md](./ERD_SESSION_SERVICE.md)** - Database: `ds_session_service`
- **2 bảng**: `delivery_sessions`, `delivery_assignments`
- Quản lý phiên giao hàng và assignments
- Tích hợp với Parcel Service và User Service

## Thống Kê Tổng Quan

| Service | Số Bảng | Database | Stack |
|---------|---------|----------|-------|
| User Service | 3 | ds_user_service | Spring Boot (Java) |
| Settings Service | 1 | ds_settings_service | Spring Boot (Java) |
| Zone Service | 14 | ds_zone_service | Node.js/Express (Prisma) |
| Parcel Service | 2 | ds_parcel_service | Spring Boot (Java) |
| Communication Service | 5 | ds_communication_service | Spring Boot (Java) |
| Session Service | 2 | ds_session_service | Spring Boot (Java) |
| **TỔNG** | **30** | - | - |

## Cross-Service References

Các tham chiếu giữa các service được mô tả chi tiết trong từng file ERD. Tổng quan:

- **User Service** được tham chiếu bởi: Parcel Service, Communication Service, Session Service, Zone Service
- **Zone Service** được tham chiếu bởi: User Service, Parcel Service
- **Parcel Service** được tham chiếu bởi: Session Service
- **Session Service** được tham chiếu bởi: Communication Service

## Lưu Ý

- Tất cả các ERD đều sử dụng Mermaid diagram format
- Các snapshot tables đã được loại trừ
- Cross-service references không có foreign key constraints
- Tất cả primary keys đều sử dụng UUID (trừ `system_settings` dùng `setting_key`)

## Cách Sử Dụng

1. **Xem tổng quan**: Đọc [ERD_COMPLETE.md](./ERD_COMPLETE.md)
2. **Xem chi tiết service**: Đọc file ERD tương ứng của service
3. **Hiểu quan hệ**: Xem phần "Cross-Service References" trong mỗi file ERD

## Render Mermaid Diagrams

Các ERD diagrams sử dụng Mermaid format và có thể được render trên:
- GitHub/GitLab (tự động render)
- VS Code với extension Mermaid Preview
- Online tools: https://mermaid.live/
- Documentation tools hỗ trợ Mermaid
