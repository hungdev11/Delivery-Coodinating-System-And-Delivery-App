package com.ds.communication_service.common.enums;

public enum ProposalActionType {
    /**
     * Mặc định: Hiển thị 2 nút [Chấp nhận] / [Từ chối].
     */
    ACCEPT_DECLINE,
    
    /**
     * Hiển thị một ô [Nhập văn bản] và nút [Gửi].
     */
    TEXT_INPUT,
    
    /**
     * Hiển thị một nút [Chọn ngày] (mở ra lịch).
     */
    DATE_PICKER,
    
    /**
     * Hiển thị một danh sách các lựa chọn (radio button).
     */
    SINGLE_CHOICE,
    
    /**
     * Chỉ thông báo: Hiển thị một nút [Đã hiểu] hoặc [OK].
     */
    INFO_ONLY 
}
