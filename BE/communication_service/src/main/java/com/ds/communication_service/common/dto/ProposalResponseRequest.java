package com.ds.communication_service.common.dto;

import lombok.Data;

@Data
public class ProposalResponseRequest {
    
    /**
     * Dữ liệu kết quả do người dùng phản hồi.
     * Ví dụ: "ACCEPTED", "DECLINED", "Lý do của tôi", "2025-11-10"
     */
    private String resultData;
}

