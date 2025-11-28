package com.ds.communication_service.common.enums;

public enum ContentType {
    TEXT, 
    IMAGE,
    // ...
    
    /**
     * Tin nhắn này là một "đề nghị tương tác".
     * Client phải đọc trường 'proposal_id' của Message
     * để lấy thông tin và render card tương tác.
     */
    INTERACTIVE_PROPOSAL,
    
    /**
     * Tin nhắn thông báo đơn hàng đã hoàn thành giao hàng.
     * Content chứa JSON với thông tin parcel (parcelId, parcelCode, completedAt, etc.)
     */
    DELIVERY_COMPLETED,
    
    /**
     * Tin nhắn thông báo đơn hàng đã chuyển sang trạng thái SUCCEEDED (hoàn thành).
     * Content chứa JSON với thông tin parcel và nguồn gốc (confirmedBy: user confirm hay tự động)
     */
    DELIVERY_SUCCEEDED
}
