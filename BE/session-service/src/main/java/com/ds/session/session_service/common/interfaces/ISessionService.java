package com.ds.session.session_service.common.interfaces;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.entities.dto.response.TaskSessionResponse;

public interface ISessionService {
    void acceptTask(UUID taskId, UUID deliveryManId);
    TaskSessionResponse completeTask(UUID taskId, UUID deliveryManId, RouteInfo routeInfo);
    TaskSessionResponse deliveryFailed(UUID taskId, UUID deliveryManId, String reason, RouteInfo routeInfo);
    TaskSessionResponse changeTimeWindow(UUID taskId, UUID deliveryManId, LocalDateTime startTime, LocalDateTime endTime);
    PageResponse<TaskSessionResponse> getTasksOfDeliveryMan(UUID deliveryManId, LocalDateTime beginTime, LocalDateTime endTime, int page, int size, String sortBy, String direction);
    PageResponse<TaskSessionResponse> getTaskTodayOfDeliveryMan(UUID deliveryManId, int page, int size, String sortBy, String direction);
}