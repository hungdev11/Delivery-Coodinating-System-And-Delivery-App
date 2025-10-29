package com.ds.session.session_service.business.v1.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.app_context.models.DeliverySession; 
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentRepository;
import com.ds.session.session_service.app_context.repositories.DeliverySessionRepository; 
import com.ds.session.session_service.application.client.parcelclient.ParcelServiceClient;
import com.ds.session.session_service.application.client.parcelclient.response.ParcelResponse;
import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.enums.AssignmentStatus;
import com.ds.session.session_service.common.enums.ParcelEvent;
import com.ds.session.session_service.common.enums.SessionStatus; 
import com.ds.session.session_service.common.exceptions.ResourceNotFound;
import com.ds.session.session_service.common.interfaces.IDeliveryAssignmentService;
import com.ds.session.session_service.common.mapper.ParcelMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper; 

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeliveryAssignmentService implements IDeliveryAssignmentService {

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliverySessionRepository deliverySessionRepository; 
    private final ParcelServiceClient parcelServiceClient;
    private final ParcelMapper parcelMapper; 
    private final ObjectMapper objectMapper; 

    @Override
    public DeliveryAssignmentResponse completeTask(UUID parcelId, UUID deliveryManId, RouteInfo routeInfo) {
        return updateTaskState(
            parcelId,
            deliveryManId,
            routeInfo,
            AssignmentStatus.COMPLETED,      // newStatus
            ParcelEvent.DELIVERY_SUCCESSFUL, // parcelEvent
            null                  // failReason
        );
    }

    @Override
    public DeliveryAssignmentResponse deliveryFailed(UUID parcelId, UUID deliveryManId, String reason, RouteInfo routeInfo) {
        return updateTaskState(
            parcelId,
            deliveryManId,
            routeInfo,
            AssignmentStatus.FAILED,           // newStatus
            ParcelEvent.CAN_NOT_DELIVERY,      // parcelEvent 
            reason                             // failReason
        );
    }

    @Override
    public DeliveryAssignmentResponse rejectedByCustomer(UUID parcelId, UUID deliveryManId, String reason, RouteInfo routeInfo) {
        return updateTaskState(
            parcelId,
            deliveryManId,
            routeInfo,
            AssignmentStatus.COMPLETED,       // newStatus
            ParcelEvent.CUSTOMER_REJECT,      // parcelEvent 
            reason                            // failReason
        );
    }

    private DeliveryAssignmentResponse updateTaskState(UUID parcelId, UUID deliveryManId, RouteInfo routeInfo,
                                                       AssignmentStatus newStatus, ParcelEvent parcelEvent, String failReason) {
        
        // 1. Tìm session đang hoạt động của shipper
        DeliverySession session = getActiveSessionOrFail(deliveryManId.toString());
        // 2. Tìm task (đơn hàng) trong session đó
        DeliveryAssignment assignment = getAssignmentInSessionOrFail(session.getId(), parcelId.toString());
        
        // 3. Kiểm tra trạng thái
        ensureStatusIsProcessing(assignment); 
        
        // 4. Cập nhật thông tin tuyến đường
        setRouteInfo(assignment, routeInfo);
        
        // 5. Đồng bộ với Parcel service
        ParcelInfo parcel = updateParcelStatusAndMap(parcelId, parcelEvent);

        // 6. Cập nhật trạng thái (tham số hóa)
        assignment.setStatus(newStatus);
        assignment.setFailReason(failReason); 
        
        // 7. Lưu
        deliveryAssignmentRepository.save(assignment);

        // 8. Trả về DTO
        // TODO: Lấy SĐT shipper và tên người nhận từ User-Service.
        String deliveryManPhone = null; 
        String receiverName = null; 
        
        return DeliveryAssignmentResponse.from(assignment, parcel, assignment.getSession(), deliveryManPhone, receiverName);
    }


    /**
     * getDailyTasks giờ sẽ trả về các task của SESSION ĐANG HOẠT ĐỘNG
     */
    @Override
    public List<DeliveryAssignmentResponse> getDailyTasks(UUID deliveryManId) {
        // 1. Tìm session đang hoạt động
        DeliverySession activeSession = deliverySessionRepository
            .findByDeliveryManIdAndStatus(deliveryManId.toString(), SessionStatus.IN_PROGRESS)
            .orElse(null); // Không tìm thấy session nào
            
        if (activeSession == null || activeSession.getAssignments() == null) {
            log.info("No active session found for delivery man {}", deliveryManId);
            return Collections.emptyList();
        }

        // 2. Lấy danh sách task từ session
        List<DeliveryAssignment> tasks = activeSession.getAssignments();
        
        // 3. xử lý bulk
        return getEnrichedTasks(tasks);
    }

    /**
     * getTasksBetween giờ sẽ tìm các SESSION đã hoàn thành trong khoảng thời gian
     */
    @Override
    public List<DeliveryAssignmentResponse> getTasksBetween(UUID deliveryManId, LocalDate start, LocalDate end) {
        List<DeliverySession> sessions;
        if (start == null || end == null) {
            // Lấy tất cả session (đã xong) của shipper
            sessions = deliverySessionRepository.findAllByDeliveryManIdAndStatus(
                deliveryManId.toString(), SessionStatus.COMPLETED);
        } else {
            // Lấy các session đã xong trong khoảng thời gian
            sessions = deliverySessionRepository.findAllByDeliveryManIdAndStatusAndEndTimeBetween(
                deliveryManId.toString(), 
                SessionStatus.COMPLETED,
                start.atStartOfDay(),
                end.atTime(LocalTime.MAX));
        }
        
        // Lấy tất cả các task từ các session tìm được
        List<DeliveryAssignment> tasks = sessions.stream()
            .flatMap(session -> session.getAssignments().stream())
            .toList();

        return getEnrichedTasks(tasks);
    }
    
    private List<DeliveryAssignmentResponse> getEnrichedTasks(List<DeliveryAssignment> tasks) {
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Thu thập tất cả các parcelId
        List<UUID> parcelIds = tasks.stream()
            .map(t -> UUID.fromString(t.getParcelId()))
            .distinct()
            .toList();

        Map<String, ParcelResponse> parcelResponseMap;
        
        try {
            parcelResponseMap = parcelServiceClient.fetchParcelsBulk(parcelIds);
        } catch (Exception e) {
            log.error("Failed to fetch bulk parcel info: {}", e.getMessage());
            parcelResponseMap = Collections.emptyMap(); // Trả về rỗng nếu lỗi
        }

        Map<String, ParcelInfo> parcelInfoMap = parcelResponseMap.entrySet().stream()
            .filter(entry -> entry.getValue() != null) // Lọc ra các entry bị lỗi (nếu có)
            .collect(Collectors.toMap(
                Map.Entry::getKey, // Key là parcelId (String)
                entry -> parcelMapper.toParcelInfo(entry.getValue()) // Value là ParcelInfo đã map
            ));
        
        // 3. TODO: Lấy SĐT Shipper
        String deliveryManPhone = null; // Tạm thời
        
        // 4. TODO: Lấy tên Người nhận
        String receiverName = null; // Tạm thời

        // 5. Map dữ liệu
        List<DeliveryAssignmentResponse> res = tasks.stream().map(t -> {
            ParcelInfo parcelInfo = parcelInfoMap.get(t.getParcelId());
            if (parcelInfo == null) {
                return null; // Bỏ qua nếu không lấy được thông tin
            }
            
            return DeliveryAssignmentResponse.from(t, parcelInfo, t.getSession(), deliveryManPhone, receiverName);
        
        }).filter(response -> response != null) // Lọc ra các entry bị null
          .toList();

        return res;
    }

    // --- UTILITY METHODS ---
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize route info object to JSON string.", e);
            throw new RuntimeException("Failed to serialize route info", e);
        }
    }

    private void setRouteInfo(DeliveryAssignment deliveryAssignment, RouteInfo routeInfo) {
        if (routeInfo == null) return;
        deliveryAssignment.setDistanceM(routeInfo.getDistanceM());
        deliveryAssignment.setDurationS(routeInfo.getDurationS());
        deliveryAssignment.setWaypoints(toJson(routeInfo.getWaypoints())); 
    }
    
    private DeliverySession getActiveSessionOrFail(String deliveryManId) {
        return deliverySessionRepository.findByDeliveryManIdAndStatus(deliveryManId, SessionStatus.IN_PROGRESS)
            .orElseThrow(() -> new ResourceNotFound("No active delivery session found for shipper."));
    }

    private DeliveryAssignment getAssignmentInSessionOrFail(UUID sessionId, String parcelId) {
        return deliveryAssignmentRepository.findBySession_IdAndParcelId(sessionId, parcelId)
            .orElseThrow(() -> new ResourceNotFound("Assignment for parcel " + parcelId + " not found in session " + sessionId));
    }

    private void ensureStatusIsProcessing(DeliveryAssignment assignment) {
        if (!AssignmentStatus.IN_PROGRESS.equals(assignment.getStatus())) {
            throw new IllegalStateException("Can not finish assignment that is not currently IN_PROGRESS.");
        }
    }

    private ParcelInfo updateParcelStatusAndMap(UUID parcelId, ParcelEvent event) {
        ParcelResponse response = parcelServiceClient.changeParcelStatus(parcelId.toString(), event);
        return parcelMapper.toParcelInfo(response);
    }
}

