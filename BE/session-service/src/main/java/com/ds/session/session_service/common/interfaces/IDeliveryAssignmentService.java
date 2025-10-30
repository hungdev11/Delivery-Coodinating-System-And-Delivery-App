package com.ds.session.session_service.common.interfaces;

import java.util.List;
import java.util.UUID;

import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;

/**
 * Interface (đã chính xác từ lần trước)
 */
public interface IDeliveryAssignmentService {
    
    DeliveryAssignmentResponse completeTask(UUID parcelId, UUID deliveryManId, RouteInfo routeInfo);
    
    DeliveryAssignmentResponse deliveryFailed(UUID parcelId, UUID deliveryManId, String reason, RouteInfo routeInfo);
    
    DeliveryAssignmentResponse rejectedByCustomer(UUID parcelId, UUID deliveryManId, String reason, RouteInfo routeInfo);
    
    /**
     * Lấy các task trong phiên ĐANG HOẠT ĐỘNG (phân trang và lọc)
     */
    PageResponse<DeliveryAssignmentResponse> getDailyTasks(
        UUID deliveryManId, 
        List<String> status, 
        int page, 
        int size
    );
    
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
}

