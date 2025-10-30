package com.ds.communication_service.common.enums;

/**
 * Định nghĩa các loại hành động/chức năng đặc biệt
 * mà một tin nhắn mẫu có thể có.
 */
public enum TemplateActionType {
    
    /**
     * Mặc định: Chỉ là một tin nhắn văn bản đơn giản.
     * Client chỉ việc hiển thị nội dung tin nhắn.
     */
    SIMPLE_TEXT,
    
    /**
     * Yêu cầu Client hiển thị các nút trả lời nhanh (quick reply buttons).
     * 'content' của template có thể chứa JSON hoặc một định dạng đặc biệt
     * để client có thể phân tích và hiển thị các nút.
     */
    QUICK_REPLY,
    
    /**
     * Kích hoạt một hành động/lệnh đặc biệt từ bot hoặc hệ thống.
     * Ví dụ: /transfer_to_agent, /end_chat
     */
    BOT_COMMAND
}