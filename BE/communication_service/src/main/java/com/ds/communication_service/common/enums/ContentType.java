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
    INTERACTIVE_PROPOSAL
}
