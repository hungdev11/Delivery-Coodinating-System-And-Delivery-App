package com.ds.communication_service.common.enums;

public enum ProposalStatus {
    
    /**
     * Đã gửi, đang chờ phản hồi từ người nhận.
     */
    PENDING,
    
    /**
     * Người nhận đã chấp nhận.
     */
    ACCEPTED,
    
    /**
     * Người nhận đã từ chối (chủ động).
     */
    DECLINED,
    
    /**
     * Người gửi đã hủy trước khi có phản hồi.
     */
    CANCELLED,
    
    /**
     * Hết hạn mà không có phản hồi (bị động).
     */
    EXPIRED
}
