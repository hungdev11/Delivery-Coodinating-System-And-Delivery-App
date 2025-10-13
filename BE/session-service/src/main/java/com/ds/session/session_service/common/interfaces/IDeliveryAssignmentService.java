package com.ds.session.session_service.common.interfaces;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;

public interface IDeliveryAssignmentService {
    void acceptTask(UUID taskId, UUID deliveryManId);
    DeliveryAssignmentResponse completeTask(UUID taskId, UUID deliveryManId, RouteInfo routeInfo);
    DeliveryAssignmentResponse deliveryFailed(UUID taskId, UUID deliveryManId, String reason, RouteInfo routeInfo);
    DeliveryAssignmentResponse changeTimeWindow(UUID taskId, UUID deliveryManId, LocalDateTime startTime, LocalDateTime endTime);
    PageResponse<DeliveryAssignmentResponse> getTasksOfDeliveryMan(UUID deliveryManId, LocalDateTime beginTime, LocalDateTime endTime, int page, int size, String sortBy, String direction);
    PageResponse<DeliveryAssignmentResponse> getTaskTodayOfDeliveryMan(UUID deliveryManId, int page, int size, String sortBy, String direction);
}