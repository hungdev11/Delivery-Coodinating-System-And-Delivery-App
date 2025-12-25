package com.ds.session.session_service.common.enums;

public enum AssignmentStatus {
    ASSIGNED,    // hệ thống gán
    ACCEPTED,    // shipper xác nhận
    REJECTED,    // shipper từ chối
    EXPIRED,     // hết hạn
    IN_PROGRESS, COMPLETED, FAILED, DELAYED
}
