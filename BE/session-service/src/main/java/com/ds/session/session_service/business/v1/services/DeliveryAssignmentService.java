package com.ds.session.session_service.business.v1.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentRepository;
import com.ds.session.session_service.application.client.parcelclient.ParcelServiceClient;
import com.ds.session.session_service.application.client.parcelclient.response.ParcelResponse;
import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.enums.AssignmentStatus;
import com.ds.session.session_service.common.enums.ParcelEvent;
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
    private final ParcelServiceClient parcelServiceClient;
    private final ParcelMapper parcelMapper; 
    
    private final ObjectMapper objectMapper = new ObjectMapper(); 
    
    private LocalDateTime getStartOfToday() {
        return LocalDateTime.now().toLocalDate().atStartOfDay();
    }
    
    private LocalDateTime getEndOfToday() {
        return LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
    }

    @Override
    public boolean acceptTask(UUID parcelId, UUID deliveryManId) {
        ParcelResponse parcelResponse = parcelServiceClient.fetchParcelResponse(parcelId);
        
        if (parcelResponse == null || parcelResponse.getStatus() == null || !"IN_WAREHOUSE".equals(parcelResponse.getStatus())) {
            log.warn("Cannot accept parcel {}. Status is invalid or null.", parcelId);
            return false;
        }

        final String parcelIdStr = parcelId.toString();
        final String deliveryManIdStr = deliveryManId.toString();
        
        if (isParcelAlreadyAssignedToday(deliveryManIdStr, parcelIdStr)) {
            log.warn("Parcel {} already assigned to shipper {} today.", parcelId, deliveryManId);
            return false;
        }

        parcelServiceClient.changeParcelStatus(parcelId, ParcelEvent.SCAN_QR);

        DeliveryAssignment deliveryAssignment = DeliveryAssignment.builder()
            .deliveryManId(deliveryManIdStr)
            .parcelId(parcelIdStr)
            .failReason("")
            .scanedAt(LocalDateTime.now())
            .status(AssignmentStatus.PROCESSING)
            .build();
        deliveryAssignmentRepository.save(deliveryAssignment);
        
        log.info("Successfully accepted task for parcel {} by delivery man {}.", parcelId, deliveryManId);
        return true;
    }

    @Override
    public DeliveryAssignmentResponse completeTask(UUID parcelId, UUID deliveryManId, RouteInfo routeInfo) {
        DeliveryAssignment assignment = getAssignmentOrFail(parcelId.toString(), deliveryManId.toString());
        
        ensureStatusIsProcessing(assignment);
        
        insertRouteInfo(assignment, routeInfo);
        
        ParcelInfo parcel = updateParcelStatusAndMap(parcelId, ParcelEvent.DELIVERY_SUCCESSFUL);

        assignment.setStatus(AssignmentStatus.SUCCESS);

        assignment.setFailReason(null); 

        deliveryAssignmentRepository.save(assignment);

        return DeliveryAssignmentResponse.from(assignment, parcel, "Phan Phi Hung", "Maria Akamoto");
    }

    @Override
    public DeliveryAssignmentResponse deliveryFailed(UUID parcelId, UUID deliveryManId, boolean flag, String reason, RouteInfo routeInfo) {
        DeliveryAssignment assignment = getAssignmentOrFail(parcelId.toString(), deliveryManId.toString());

        ensureStatusIsProcessing(assignment);
        
        insertRouteInfo(assignment, routeInfo);

        if (flag) {
            updateParcelStatusAndMap(parcelId, ParcelEvent.DELIVERY_SUCCESSFUL);
            ParcelInfo parcel1 = updateParcelStatusAndMap(parcelId, ParcelEvent.CUSTOMER_REJECT);
            assignment.setStatus(AssignmentStatus.SUCCESS);
            assignment.setFailReason(null); 
            deliveryAssignmentRepository.save(assignment);
            log.info(reason + "" + flag);
            return DeliveryAssignmentResponse.from(assignment, parcel1, "Phan Phi Hung", "Maria Akamoto");
        }
        
        ParcelInfo parcel = updateParcelStatusAndMap(parcelId, ParcelEvent.CAN_NOT_DELIVERY);
        assignment.setStatus(AssignmentStatus.FAILED);
        assignment.setFailReason(reason);

        deliveryAssignmentRepository.save(assignment);
        log.info(reason);
        return DeliveryAssignmentResponse.from(assignment, parcel, "Phan Phi Hung", "Maria Akamoto");
    }

    @Override
    public PageResponse<DeliveryAssignmentResponse> getDailyTasks(UUID deliveryManId, List<String> status, int page, int size) {
        return getTasks(
            deliveryManId, 
            status, 
            getStartOfToday().toLocalDate(), 
            getEndOfToday().toLocalDate(), 
            null, null, 
            page, size);
    }

    @Override
    public PageResponse<DeliveryAssignmentResponse> getTasks(
        UUID deliveryManId, 
        List<String> status, 
        LocalDate createdAtStart, 
        LocalDate createdAtEnd, 
        LocalDate completedAtStart, 
        LocalDate completedAtEnd,
        int page,
        int size
    ) {
        Specification<DeliveryAssignment> spec = Specification
            .where(AssignmentSpecification.byDeliveryManId(deliveryManId))
            .and(AssignmentSpecification.hasStatusIn(status))
            .and(AssignmentSpecification.isCreatedAtBetween(createdAtStart, createdAtEnd))
            .and(AssignmentSpecification.isCompletedAtBetween(completedAtStart, completedAtEnd));

        Pageable pageable = PageUtil.build(page, size, "createdAt", "asc", DeliveryAssignment.class);
        Page<DeliveryAssignment> taskPage = deliveryAssignmentRepository.findAll(spec, pageable);

        return PageResponse.from(taskPage.map(t -> {
            ParcelResponse response = parcelServiceClient.fetchParcelResponse(UUID.fromString(t.getParcelId()));
            ParcelInfo parcelInfo = parcelMapper.toParcelInfo(response);
            return DeliveryAssignmentResponse.from(t, parcelInfo, "0935960974", "Maria Akamoto");
        }));
    }

    // --- UTILITY METHODS ---
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            // Chuyển đổi JsonProcessingException thành RuntimeException
            log.error("Failed to serialize route info object to JSON string.", e);
            throw new RuntimeException("Failed to serialize route info", e);
        }
    }

    private void insertRouteInfo(DeliveryAssignment deliveryAssignment, RouteInfo routeInfo) {
        deliveryAssignment.setDistanceM(routeInfo.getDistanceM());
        deliveryAssignment.setDurationS(routeInfo.getDurationS());
        deliveryAssignment.setWaypoints(toJson(routeInfo.getWaypoints())); 
        deliveryAssignmentRepository.save(deliveryAssignment); 
    }
    
    private DeliveryAssignment getAssignmentOrFail(String parcelId, String deliveryManId) {
        return deliveryAssignmentRepository.findByDeliveryManIdAndParcelIdAndScanedAtBetween(
                deliveryManId, 
                parcelId, 
                getStartOfToday(), 
                getEndOfToday())
            .orElseThrow(() -> new ResourceNotFound("Delivery assignment not found for today."));
    }

    private boolean isParcelAlreadyAssignedToday(String deliveryManId, String parcelId) {
        return deliveryAssignmentRepository.existsByDeliveryManIdAndParcelIdAndScanedAtBetween(
            deliveryManId, 
            parcelId, 
            getStartOfToday(), 
            getEndOfToday());
    }

    private void ensureStatusIsProcessing(DeliveryAssignment assignment) {
        if (!AssignmentStatus.PROCESSING.equals(assignment.getStatus())) {
            throw new IllegalStateException("Can not finish assignment that is not currently PROCESSING.");
        }
    }

    private ParcelInfo updateParcelStatusAndMap(UUID parcelId, ParcelEvent event) {
        ParcelResponse response = parcelServiceClient.changeParcelStatus(parcelId, event);
        return parcelMapper.toParcelInfo(response);
    }

    
}