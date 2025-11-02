package com.ds.session.session_service.business.v1.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.entities.dto.response.ShipperInfo;
import com.ds.session.session_service.common.enums.AssignmentStatus;
import com.ds.session.session_service.common.enums.ParcelEvent;
import com.ds.session.session_service.common.enums.SessionStatus; 
import com.ds.session.session_service.common.exceptions.ResourceNotFound;
import com.ds.session.session_service.common.interfaces.IDeliveryAssignmentService;
import com.ds.session.session_service.common.mapper.ParcelMapper;
import com.ds.session.session_service.common.utils.AssignmentSpecification;
import com.ds.session.session_service.common.utils.PageUtil;
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

    @Override
    public DeliveryAssignmentResponse postponeByCustomer(UUID parcelId, UUID deliveryManId, String reason, RouteInfo routeInfo) {
        return updateTaskState(
            parcelId,
            deliveryManId,
            routeInfo,
            AssignmentStatus.COMPLETED,       // newStatus
            ParcelEvent.POSTPONE,      // parcelEvent 
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
    @Transactional(readOnly = true) // Dùng readOnly cho các hàm GET
    public PageResponse<DeliveryAssignmentResponse> getDailyTasks(
        UUID deliveryManId, List<String> status, int page, int size
    ) {
        // 1. Xây dựng đối tượng phân trang
        // (Mặc định sắp xếp theo thời gian quét (scanedAt) mới nhất)
        Pageable pageable = PageUtil.build(page, size, "scanedAt", "desc", DeliveryAssignment.class);

        // 2. Xây dựng Specification (Tiêu chí lọc)
        Specification<DeliveryAssignment> spec = Specification
            .where(AssignmentSpecification.byDeliveryManId(deliveryManId))
            .and(AssignmentSpecification.bySessionStatus(SessionStatus.IN_PROGRESS))
            .and(AssignmentSpecification.hasAssignmentStatusIn(status));

        // 3. Gọi Repository
        Page<DeliveryAssignment> tasksPage = deliveryAssignmentRepository.findAll(spec, pageable);

        // 4. Ánh xạ kết quả sang DTO
        return getEnrichedTasks(tasksPage);
    }

    /**
     * VIẾT LẠI HÀM: Sử dụng Specification và Pageable
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeliveryAssignmentResponse> getTasksBetween(
        UUID deliveryManId, List<String> status,
        String createdAtStart, String createdAtEnd,
        String completedAtStart, String completedAtEnd,
        int page, int size
    ) {
        // 1. Phân trang
        Pageable pageable = PageUtil.build(page, size, "scanedAt", "desc", DeliveryAssignment.class);

        // 2. Xây dựng Specification
        // Lọc các phiên đã KẾT THÚC (COMPLETED hoặc FAILED)
        List<SessionStatus> terminalStatuses = Arrays.asList(SessionStatus.COMPLETED, SessionStatus.FAILED);

        Specification<DeliveryAssignment> spec = Specification
            .where(AssignmentSpecification.byDeliveryManId(deliveryManId))
            //.and(AssignmentSpecification.bySessionStatusIn(terminalStatuses)) // Lọc theo trạng thái Session
            .and(AssignmentSpecification.hasAssignmentStatusIn(status)) // Lọc theo trạng thái Task
            .and(AssignmentSpecification.isCreatedAtBetween(createdAtStart, createdAtEnd)) // Lọc theo ngày tạo task
            .and(AssignmentSpecification.isCompletedAtBetween(completedAtStart, completedAtEnd)); // Lọc theo ngày hoàn thành

        // 3. Gọi Repository
        Page<DeliveryAssignment> tasksPage = deliveryAssignmentRepository.findAll(spec, pageable);
        
        // 4. Ánh xạ
        return getEnrichedTasks(tasksPage);
    }
    
    private PageResponse<DeliveryAssignmentResponse> getEnrichedTasks(Page<DeliveryAssignment> tasksPage) {
        if (tasksPage.isEmpty()) {
            return PageResponse.from(tasksPage, Collections.emptyList());
        }

        List<DeliveryAssignment> tasks = tasksPage.getContent();

        // 1. Thu thập tất cả các parcelId
        List<UUID> parcelIds = tasks.stream()
            .map(t -> UUID.fromString(t.getParcelId()))
            .distinct()
            .toList();

        Map<String, ParcelResponse> parcelResponseMap = parcelServiceClient.fetchParcelsBulk(parcelIds);
        
        Map<String, ParcelInfo> parcelInfoMap = parcelResponseMap.entrySet().stream()
            .filter(entry -> entry.getValue() != null)
            .collect(Collectors.toMap(
                Map.Entry::getKey, 
                entry -> parcelMapper.toParcelInfo(entry.getValue())
            ));
        
        // 3. TODO: Lấy SĐT Shipper
        String deliveryManPhone = null; // Tạm thời
        
        // 4. TODO: Lấy tên Người nhận
        String receiverName = null; // Tạm thời

        // 5. Map dữ liệu
        List<DeliveryAssignmentResponse> dtoList = tasks.stream().map(t -> {
            ParcelInfo parcelInfo = parcelInfoMap.get(t.getParcelId());
            if (parcelInfo == null) {
                return null; 
            }
            return DeliveryAssignmentResponse.from(t, parcelInfo, t.getSession(), deliveryManPhone, receiverName);
        
        }).filter(response -> response != null)
          .toList();

        // Trả về PageResponse (giữ nguyên thông tin phân trang)
        return PageResponse.from(tasksPage, dtoList);
    }

    public Optional<ShipperInfo> getLatestDriverIdForParcel(String parcelId) {
        log.info("Tìm kiếm tài xế gần nhất cho parcelId: {}", parcelId);

        Optional<DeliveryAssignment> latestAssignmentOpt = deliveryAssignmentRepository.findFirstByParcelIdOrderByUpdatedAtDesc(parcelId);

        if (latestAssignmentOpt.isEmpty()) {
            log.warn("Không tìm thấy assignment hợp lệ nào cho parcelId: {}", parcelId);
            return Optional.empty();
        }

        return latestAssignmentOpt.map(assignment -> {
            String driverId = assignment.getSession().getDeliveryManId();
            log.info("Tìm thấy tài xế: {} cho parcelId: {}", driverId, parcelId);
            return new ShipperInfo(driverId, "Tài xế", "0912312312");
        });
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
        log.info("parcel status: {}, event: {}", response.getStatus(), event);
        return parcelMapper.toParcelInfo(response);
    }
}

