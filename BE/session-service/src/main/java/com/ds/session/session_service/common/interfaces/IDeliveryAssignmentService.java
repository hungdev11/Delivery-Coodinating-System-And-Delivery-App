package com.ds.session.session_service.common.interfaces;

import java.util.List;
import java.util.UUID;

import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;

public interface IDeliveryAssignmentService {
    boolean acceptTask(UUID parcelId, UUID deliveryManId);
    DeliveryAssignmentResponse completeTask(UUID parcelId, UUID deliveryManId, RouteInfo routeInfo);
    DeliveryAssignmentResponse deliveryFailed(UUID parcelId, UUID deliveryManId, String reason, RouteInfo routeInfo);
    List<DeliveryAssignmentResponse> getDailyTasks(UUID deliveryManId);

}