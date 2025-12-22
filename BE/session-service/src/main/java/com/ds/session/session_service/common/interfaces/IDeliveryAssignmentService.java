package com.ds.session.session_service.common.interfaces;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.ds.session.session_service.common.entities.dto.request.CompleteTaskRequest;
import com.ds.session.session_service.common.entities.dto.request.PagingRequestV0;
import com.ds.session.session_service.common.entities.dto.request.PagingRequestV2;
import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.LatestAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.entities.dto.response.ShipperInfo;

/**
 * Interface (đã chính xác từ lần trước)
 */
public interface IDeliveryAssignmentService {
    
    DeliveryAssignmentResponse completeTask(UUID parcelId, UUID deliveryManId, CompleteTaskRequest request);    
    
    DeliveryAssignmentResponse deliveryFailed(UUID parcelId, UUID deliveryManId, String reason, RouteInfo routeInfo);
    
    DeliveryAssignmentResponse rejectedByCustomer(UUID parcelId, UUID deliveryManId, String reason, RouteInfo routeInfo);

    DeliveryAssignmentResponse postponeByCustomer(UUID parcelId, UUID deliveryManId, String reason, RouteInfo routeInfo);
    
    /**
     * Postpone assignment directly by assignmentId.
     * This is used when we already have the assignmentId (e.g., from proposal response).
     * 
     * @param assignmentId Assignment ID to postpone
     * @param request Postpone request with reason, route info, postpone datetime, and moveToEnd flag
     * @return Updated assignment response
     */
    DeliveryAssignmentResponse postponeByAssignmentId(UUID assignmentId, 
        com.ds.session.session_service.common.entities.dto.request.PostponeAssignmentRequest request);
    
    /**
     * Get active assignment ID for a parcel and delivery man.
     * Returns the assignmentId of the active assignment (in CREATED or IN_PROGRESS session).
     * This is used by Communication Service to find assignmentId before calling postpone endpoint.
     */
    Optional<UUID> getActiveAssignmentId(String parcelId, String deliveryManId);
    
    /**
     * Update assignment status by sessionId and assignmentId
     * This is used by API gateway for nested queries
     */
    DeliveryAssignmentResponse updateAssignmentStatus(UUID sessionId, UUID assignmentId, 
                                                      com.ds.session.session_service.common.entities.dto.request.UpdateAssignmentStatusRequest request);
    
    /**
     * Lấy các task trong phiên ĐANG HOẠT ĐỘNG (phân trang và lọc) - V1
     */
    PageResponse<DeliveryAssignmentResponse> getDailyTasks(
        UUID deliveryManId, 
        List<String> status, 
        int page, 
        int size
    );
    
    /**
     * Lấy các task của một session cụ thể theo sessionId (phân trang).
     */
    PageResponse<DeliveryAssignmentResponse> getTasksBySessionId(
        UUID sessionId,
        int page,
        int size
    );
    
    /**
     * V0: Lấy các task với paging đơn giản (không có dynamic filters)
     */
    PageResponse<DeliveryAssignmentResponse> getDailyTasksV0(PagingRequestV0 request);
    
    /**
     * V2: Lấy các task với enhanced filtering (operations between pairs)
     */
    PageResponse<DeliveryAssignmentResponse> getDailyTasksV2(PagingRequestV2 request);
    
    /**
     * Lấy lịch sử task (phân trang và lọc)
     */
    PageResponse<DeliveryAssignmentResponse> getTasksBetween(
        UUID deliveryManId, 
        List<String> status,
        String createdAtStart, String createdAtEnd,
        String completedAtStart, String completedAtEnd,
        int page, int size
    );

    Optional<ShipperInfo> getLatestDriverIdForParcel(String parcelId);

    Optional<LatestAssignmentResponse> getLatestAssignmentForParcel(String parcelId);
}
