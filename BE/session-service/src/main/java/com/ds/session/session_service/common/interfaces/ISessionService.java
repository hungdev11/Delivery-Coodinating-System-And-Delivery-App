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
     * Tạo phiên ở trạng thái CREATED (chuẩn bị nhận đơn).
     * @param deliveryManId ID của shipper
     * @return Thông tin phiên vừa được tạo
     */
    SessionResponse createSessionPrepared(String deliveryManId);
    
    /**
     * Chuyển phiên từ CREATED sang IN_PROGRESS (bắt đầu giao hàng).
     * @param sessionId ID của phiên
     * @return Thông tin phiên sau khi cập nhật
     */
    SessionResponse startSession(UUID sessionId);
    
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
    
    /**
     * Lấy phiên đang hoạt động (CREATED hoặc IN_PROGRESS) của shipper.
     * @param deliveryManId ID của shipper
     * @return Thông tin phiên đang hoạt động, hoặc null nếu không có
     */
    SessionResponse getActiveSession(String deliveryManId);
}
