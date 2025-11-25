package com.ds.session.session_service.common.interfaces;

import java.util.UUID;

import com.ds.session.session_service.common.entities.dto.common.PagedData;
import com.ds.session.session_service.common.entities.dto.request.CreateSessionRequest;
import com.ds.session.session_service.common.entities.dto.request.PagingRequestV2;
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
    
    /**
     * Lấy tất cả sessions của một shipper.
     * @param deliveryManId ID của shipper
     * @param excludeParcelId (Optional) ParcelId để exclude - không trả về sessions chứa parcel này
     * @return Danh sách sessions của shipper
     */
    java.util.List<SessionResponse> getAllSessionsForDeliveryMan(String deliveryManId, String excludeParcelId);
    
    /**
     * Search delivery sessions with V2 enhanced filtering
     * @param request Paging request with filters, sorts, pagination
     * @return PagedData with sessions
     */
    PagedData<SessionResponse> searchSessionsV2(PagingRequestV2 request);
    
    /**
     * Calculate delivery time for a list of parcels
     * Used to validate if postpone time is within session time
     * @param request Request with parcel IDs and optional current location
     * @return Delivery time response with estimated completion time
     */
    com.ds.session.session_service.common.entities.dto.response.DeliveryTimeResponse calculateDeliveryTime(
        com.ds.session.session_service.common.entities.dto.request.CalculateDeliveryTimeRequest request
    );
    
    /**
     * Transfer a parcel from current shipper to another shipper
     * Only allows transferring ON_ROUTE parcels
     * @param fromDeliveryManId ID of shipper transferring the parcel
     * @param request Request containing parcelId and targetSessionId
     * @return Assignment response for the new assignment
     */
    AssignmentResponse transferParcel(String fromDeliveryManId, 
        com.ds.session.session_service.common.entities.dto.request.TransferParcelRequest request);
    
    /**
     * Accept a transferred parcel by scanning source session QR
     * @param toDeliveryManId ID of shipper accepting the parcel
     * @param request Request containing sourceSessionId and parcelId
     * @return Assignment response for the new assignment
     */
    AssignmentResponse acceptTransferredParcel(String toDeliveryManId,
        com.ds.session.session_service.common.entities.dto.request.AcceptTransferredParcelRequest request);
}
