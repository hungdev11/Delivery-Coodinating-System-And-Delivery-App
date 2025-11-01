package com.ds.deliveryapp.clients.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProposalResponseRequest {
    /**
     * Dữ liệu kết quả do người dùng phản hồi.
     * Ví dụ: "ACCEPTED", "DECLINED", "Lý do của tôi", "2025-11-10"
     */
    private String resultData;
}
