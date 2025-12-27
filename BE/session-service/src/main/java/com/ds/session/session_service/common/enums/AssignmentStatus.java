package com.ds.session.session_service.common.enums;

/**
 * AssignmentStatus enum với flow mới:
 * PENDING -> ACCEPTED -> IN_PROGRESS -> COMPLETED/FAILED
 * 
 * PENDING: Chờ shipper nhận (admin đã tạo assignment)
 * ACCEPTED: Shipper đã xác nhận nhận task
 * IN_PROGRESS: Đang trong quá trình giao hàng
 * COMPLETED: Hoàn thành
 * FAILED: Thất bại
 * 
 * Legacy statuses (giữ lại để backward compatibility):
 * ASSIGNED: Tương đương PENDING (deprecated, dùng PENDING thay thế)
 * REJECTED: Shipper từ chối
 * EXPIRED: Hết hạn
 * DELAYED: Trễ
 */
public enum AssignmentStatus {
    PENDING,     // Chờ shipper nhận (admin đã tạo assignment) - NEW
    ASSIGNED,    // hệ thống gán (deprecated, dùng PENDING)
    ACCEPTED,    // shipper xác nhận
    REJECTED,    // shipper từ chối
    EXPIRED,     // hết hạn
    IN_PROGRESS, 
    COMPLETED, 
    FAILED, 
    DELAYED
}
