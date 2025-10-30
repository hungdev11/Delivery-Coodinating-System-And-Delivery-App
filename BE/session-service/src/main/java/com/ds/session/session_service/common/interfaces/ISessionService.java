package com.ds.session.session_service.common.interfaces;

import java.util.UUID;

import com.ds.session.session_service.common.entities.dto.request.CreateSessionRequest;
import com.ds.session.session_service.common.entities.dto.response.AssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.SessionResponse;

public interface ISessionService {
    /**
     * Xử lý logic quét đơn hàng:
     * 1. Nếu shipper chưa có phiên: Tạo phiên mới và thêm task.
     * 2. Nếu shipper đã có phiên: Thêm task vào phiên hiện tại.
     * @param deliveryManId ID của shipper
     * @param parcelId ID của đơn hàng
     * @return Thông tin của task (Assignment) vừa được tạo.
     */
    AssignmentResponse acceptParcelToSession(String deliveryManId, String parcelId);
    
    SessionResponse createSession(CreateSessionRequest request);
    /**
     * Lấy thông tin chi tiết một phiên.
     */
    SessionResponse getSessionById(UUID sessionId);

    /**
     * Hoàn thành một phiên (khi tất cả các task đã xong).
     */
    SessionResponse completeSession(UUID sessionId);

    /**
     * Hủy một phiên do sự cố (shipper báo hỏng xe...).
     */
    SessionResponse failSession(UUID sessionId, String reason);
}

