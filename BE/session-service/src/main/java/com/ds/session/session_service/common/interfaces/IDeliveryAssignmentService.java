package com.ds.session.session_service.common.interfaces;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ds.session.session_service.common.entities.dto.request.PagingRequestV0;
import com.ds.session.session_service.common.entities.dto.request.PagingRequestV2;
import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.entities.dto.response.ShipperInfo;

/**
 * Interface (đã chính xác từ lần trước)
 */
public interface IDeliveryAssignmentService {
    
    DeliveryAssignmentResponse completeTask(UUID parcelId, UUID deliveryManId, RouteInfo routeInfo);
    
    DeliveryAssignmentResponse deliveryFailed(UUID parcelId, UUID deliveryManId, String reason, RouteInfo routeInfo);
    
    DeliveryAssignmentResponse rejectedByCustomer(UUID parcelId, UUID deliveryManId, String reason, RouteInfo routeInfo);

    DeliveryAssignmentResponse postponeByCustomer(UUID parcelId, UUID deliveryManId, String reason, RouteInfo routeInfo);
    
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
}

