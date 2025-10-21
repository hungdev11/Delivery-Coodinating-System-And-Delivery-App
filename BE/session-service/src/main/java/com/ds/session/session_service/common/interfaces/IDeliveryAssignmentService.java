package com.ds.session.session_service.common.interfaces;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;

public interface IDeliveryAssignmentService {
    boolean acceptTask(UUID parcelId, UUID deliveryManId);
    DeliveryAssignmentResponse completeTask(UUID parcelId, UUID deliveryManId, RouteInfo routeInfo);
    PageResponse<DeliveryAssignmentResponse> getDailyTasks(UUID deliveryManId, List<String> status, int page, int size);
    PageResponse<DeliveryAssignmentResponse> getTasks(
        UUID deliveryManId, 
        List<String> status, 
        LocalDate createdAtStart, 
        LocalDate createdAtEnd, 
        LocalDate completedAtStart, 
        LocalDate completedAtEnd,
        int page,
        int size
    );
    DeliveryAssignmentResponse deliveryFailed(UUID parcelId, UUID deliveryManId, boolean flag, String reason, RouteInfo routeInfo);
}