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
import com.ds.session.session_service.app_context.models.DeliveryProof;
import com.ds.session.session_service.app_context.models.DeliverySession;
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentRepository;
import com.ds.session.session_service.app_context.repositories.DeliverySessionRepository;
import com.ds.session.session_service.application.client.parcelclient.ParcelServiceClient;
import com.ds.session.session_service.application.client.parcelclient.response.ParcelResponse;
import com.ds.session.session_service.infrastructure.kafka.ParcelEventPublisher;
import com.ds.session.session_service.infrastructure.kafka.EventProducer;
import com.ds.session.session_service.common.entities.dto.event.AssignmentCompletedEvent;
import com.ds.session.session_service.common.entities.dto.request.CompleteTaskRequest;
import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.request.UpdateAssignmentStatusRequest;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.LatestAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.entities.dto.response.ShipperInfo;
import com.ds.session.session_service.common.enums.AssignmentStatus;
import com.ds.session.session_service.common.enums.ParcelEvent;
import com.ds.session.session_service.common.enums.ProofType;
import com.ds.session.session_service.common.enums.SessionStatus;
import com.ds.session.session_service.common.exceptions.ResourceNotFound;
import com.ds.session.session_service.common.interfaces.IDeliveryAssignmentService;
import com.ds.session.session_service.common.interfaces.ISessionService;
import com.ds.session.session_service.common.mapper.ParcelMapper;
import com.ds.session.session_service.common.utils.AssignmentSpecification;
import com.ds.session.session_service.common.utils.PageUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.ds.session.session_service.app_context.repositories.DeliveryProofRepository;
import com.ds.session.session_service.common.entities.dto.request.FailTaskRequest;

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
    private final ParcelEventPublisher parcelEventPublisher;
    private final EventProducer eventProducer;
    private final ParcelMapper parcelMapper;
    private final ObjectMapper objectMapper;
    private final ISessionService sessionService; // Inject to call completeSession
    private final DeliveryProofRepository deliveryProofRepository;
    private final com.ds.session.session_service.app_context.repositories.DeliveryConfirmationPointRepository confirmationPointRepository;

    private void uploadProof(
        DeliveryAssignment assignment,
        ProofType type,
        List<String> imageUrls
    ) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("Proof images are required");
        }

        if (imageUrls.size() > 6) {
            throw new IllegalArgumentException("Maximum 6 proof images allowed");
        }

        // Ensure assignment is managed - reload to get fresh entity
        DeliveryAssignment managedAssignment = deliveryAssignmentRepository.findById(assignment.getId())
            .orElseThrow(() -> new IllegalStateException("Assignment not found: " + assignment.getId()));

        // Get deliveryManId from session (ensure session is loaded)
        String deliveryManId = managedAssignment.getSession().getDeliveryManId();

        // Persist proofs directly to ensure assignment_id is set correctly
        for (String url : imageUrls) {
            DeliveryProof proof = DeliveryProof.builder()
                .type(type)
                .mediaUrl(url)
                .mediaPublicId(UUID.randomUUID().toString())
                .confirmedBy(deliveryManId)
                .createdAt(LocalDateTime.now())
                .build();

            // Set assignment BEFORE persisting - this is critical!
            proof.setAssignment(managedAssignment);
            
            // Persist proof directly to ensure assignment_id is set
            deliveryProofRepository.save(proof);
            
            // Also add to collection to maintain bidirectional relationship
            managedAssignment.getProofs().add(proof);
        }

        // Save the managed assignment to update updatedAt timestamp
        deliveryAssignmentRepository.save(managedAssignment);
    }

    @Override
    public DeliveryAssignmentResponse completeTask(UUID parcelId, UUID deliveryManId, CompleteTaskRequest request) {
        Optional<DeliveryAssignment> assignmentOpt = deliveryAssignmentRepository
                    .findInProgressAssignmentByParcelIdAndDeliveryManId(parcelId.toString(), deliveryManId.toString());
        
        if (assignmentOpt.isEmpty()) {
            log.error("[session-service] ... Not found ...");
            throw new ResourceNotFound("IN_PROGRESS assignment not found...");
        }

        DeliveryAssignment assignment = assignmentOpt.get();

        uploadProof(assignment, ProofType.DELIVERED, request.getProofImageUrls());

        return updateTaskState(
                parcelId,
                deliveryManId,
                request.getRouteInfo(),
                AssignmentStatus.COMPLETED,
                ParcelEvent.DELIVERY_SUCCESSFUL,
                null
        );
    }

    @Override
    public DeliveryAssignmentResponse completeTaskByAssignmentId(UUID assignmentId, CompleteTaskRequest request) {
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFound("Assignment not found: " + assignmentId));
        
        // Validate assignment is in progress
        if (assignment.getStatus() != AssignmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Assignment is not in IN_PROGRESS status. Current status: " + assignment.getStatus());
        }

        uploadProof(assignment, ProofType.DELIVERED, request.getProofImageUrls());

        // Save confirmation point if location provided
        if (request.getCurrentLat() != null && request.getCurrentLon() != null) {
            saveConfirmationPoint(assignment, request.getCurrentLat(), request.getCurrentLon(), 
                request.getCurrentTimestamp(), "DELIVERED");
        }

        // Get parcelId and deliveryManId from assignment
        UUID parcelId = UUID.fromString(assignment.getParcelId());
        UUID deliveryManId = UUID.fromString(assignment.getSession().getDeliveryManId());

        return updateTaskState(
                parcelId,
                deliveryManId,
                request.getRouteInfo(),
                AssignmentStatus.COMPLETED,
                ParcelEvent.DELIVERY_SUCCESSFUL,
                null
        );
    }

    @Override
    public DeliveryAssignmentResponse failTaskByAssignmentId(UUID assignmentId, FailTaskRequest request) {
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFound("Assignment not found: " + assignmentId));
        
        // Validate assignment is in progress
        if (assignment.getStatus() != AssignmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Assignment is not in IN_PROGRESS status. Current status: " + assignment.getStatus());
        }

        uploadProof(assignment, ProofType.FAILED, request.getProofImageUrls());

        // Save confirmation point if location provided
        if (request.getCurrentLat() != null && request.getCurrentLon() != null) {
            saveConfirmationPoint(assignment, request.getCurrentLat(), request.getCurrentLon(), 
                request.getCurrentTimestamp(), "FAILED");
        }

        // Get parcelId and deliveryManId from assignment
        UUID parcelId = UUID.fromString(assignment.getParcelId());
        UUID deliveryManId = UUID.fromString(assignment.getSession().getDeliveryManId());

        return updateTaskState(
                parcelId,
                deliveryManId,
                request.getRouteInfo(),
                AssignmentStatus.FAILED,
                ParcelEvent.POSTPONE,
                request.getFailReason()
        );
    }

    @Override
    public DeliveryAssignmentResponse returnToWarehouse(UUID assignmentId, CompleteTaskRequest request) {
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFound("Assignment not found: " + assignmentId));
        
        // Validate assignment is FAILED or DELAYED (only these need to return to warehouse)
        if (assignment.getStatus() != AssignmentStatus.FAILED && 
            assignment.getStatus() != AssignmentStatus.DELAYED) {
            throw new IllegalStateException("Only FAILED or DELAYED assignments can be returned to warehouse. Current status: " + assignment.getStatus());
        }

        // Upload proofs with type RETURNED
        uploadProof(assignment, ProofType.RETURNED, request.getProofImageUrls());

        // Save confirmation point if location provided
        if (request.getCurrentLat() != null && request.getCurrentLon() != null) {
            saveConfirmationPoint(assignment, request.getCurrentLat(), request.getCurrentLon(), 
                request.getCurrentTimestamp(), "RETURNED");
        }

        // Get parcel info for response (don't update parcel status - keep it as is)
        UUID parcelId = UUID.fromString(assignment.getParcelId());
        ParcelInfo parcel = null;
        String receiverName = null;
        
        try {
            ParcelResponse parcelResponse = parcelServiceClient.fetchParcelResponse(parcelId.toString());
            if (parcelResponse != null) {
                parcel = parcelMapper.toParcelInfo(parcelResponse);
                receiverName = parcel != null ? parcel.getReceiverName() : null;
            }
        } catch (Exception e) {
            log.debug("[session-service] [DeliveryAssignmentService.returnToWarehouse] Failed to fetch parcel info for parcel {}: {}", parcelId, e.getMessage());
            // Continue without parcel info - response will have null parcel info
        }

        log.debug("Recorded return to warehouse for assignment {}, parcel {}", assignmentId, parcelId);
        
        // Return response without changing assignment status
        return DeliveryAssignmentResponse.from(assignment, parcel, assignment.getSession(), null, receiverName);
    }

    @Override
    public DeliveryAssignmentResponse deliveryFailed(UUID parcelId, UUID deliveryManId, String reason,
            RouteInfo routeInfo) {
        return updateTaskState(
                parcelId,
                deliveryManId,
                routeInfo,
                AssignmentStatus.FAILED, // newStatus
                ParcelEvent.CAN_NOT_DELIVERY, // parcelEvent
                reason // failReason
        );
    }

    @Override
    public DeliveryAssignmentResponse rejectedByCustomer(UUID parcelId, UUID deliveryManId, String reason,
            RouteInfo routeInfo) {
        return updateTaskState(
                parcelId,
                deliveryManId,
                routeInfo,
                AssignmentStatus.FAILED, // newStatus
                ParcelEvent.CUSTOMER_REJECT, // parcelEvent
                reason // failReason
        );
    }

    @Override
    public DeliveryAssignmentResponse postponeByCustomer(UUID parcelId, UUID deliveryManId, String reason,
            RouteInfo routeInfo) {
        log.debug("Postponing parcel {} for delivery man {} with reason: {}", parcelId, deliveryManId, reason);
        try {
            // First, try to find IN_PROGRESS assignment
            Optional<DeliveryAssignment> assignmentOpt = deliveryAssignmentRepository
                    .findInProgressAssignmentByParcelIdAndDeliveryManId(parcelId.toString(), deliveryManId.toString());

            if (assignmentOpt.isPresent()) {
                // Found IN_PROGRESS assignment, proceed with postpone
                // Set assignment to FAILED when client accepts postpone request
                return updateTaskState(
                        parcelId,
                        deliveryManId,
                        routeInfo,
                        AssignmentStatus.FAILED, // newStatus - Set to FAILED when client accepts postpone
                        ParcelEvent.POSTPONE, // parcelEvent - Changes parcel from ON_ROUTE to DELAY
                        reason // failReason
                );
            } else {
                // No IN_PROGRESS assignment found, check if already DELAYED (idempotent)
                Optional<DeliveryAssignment> delayedAssignmentOpt = deliveryAssignmentRepository
                        .findFirstByParcelIdOrderByUpdatedAtDesc(parcelId.toString())
                        .filter(da -> da.getSession().getDeliveryManId().equals(deliveryManId.toString()))
                        .filter(da -> da.getStatus() == AssignmentStatus.DELAYED);

                if (delayedAssignmentOpt.isPresent()) {
                    DeliveryAssignment delayedAssignment = delayedAssignmentOpt.get();
                    log.debug(
                            "Assignment {} for parcel {} is already DELAYED. Returning existing assignment (idempotent operation).",
                            delayedAssignment.getId(), parcelId);
                    // Update reason if provided
                    if (reason != null && !reason.isEmpty()) {
                        delayedAssignment.setFailReason(reason);
                        deliveryAssignmentRepository.save(delayedAssignment);
                    }
                    // Fetch parcel info to create response
                    ParcelInfo parcel = null;
                    String receiverName = null;
                    try {
                        ParcelResponse parcelResponse = parcelServiceClient.fetchParcelResponse(parcelId.toString());
                        if (parcelResponse != null) {
                            parcel = parcelMapper.toParcelInfo(parcelResponse);
                            receiverName = parcel != null ? parcel.getReceiverName() : null;
                        }
                    } catch (Exception e) {
                        log.debug("[session-service] [DeliveryAssignmentService.postponeParcel] Failed to fetch parcel info for already DELAYED assignment: {}", e.getMessage());
                    }
                    return DeliveryAssignmentResponse.from(delayedAssignment, parcel, delayedAssignment.getSession(),
                            null, receiverName);
                }

                // NEW: Check if there is a FAILED assignment that can be transitioned to
                // DELAYED
                // This handles cases where driver marked as FAILED, but then Customer requests
                // Postpone
                Optional<DeliveryAssignment> failedAssignmentOpt = deliveryAssignmentRepository
                        .findFirstByParcelIdOrderByUpdatedAtDesc(parcelId.toString())
                        .filter(da -> da.getSession().getDeliveryManId().equals(deliveryManId.toString()))
                        .filter(da -> da.getStatus() == AssignmentStatus.FAILED);

                if (failedAssignmentOpt.isPresent()) {
                    DeliveryAssignment failedAssignment = failedAssignmentOpt.get();
                    log.debug(
                            "[session-service] [DeliveryAssignmentService.postponeParcel] Found FAILED assignment {} for parcel {}. Keeping as FAILED (client accepted postpone).",
                            failedAssignment.getId(), parcelId);

                    // Keep assignment as FAILED when client accepts postpone request
                    // Just update failReason and routeInfo if needed
                    failedAssignment.setFailReason(reason);
                    if (routeInfo != null) {
                        setRouteInfo(failedAssignment, routeInfo);
                    }

                    deliveryAssignmentRepository.save(failedAssignment);

                    // Publish event to change parcel status to DELAY
                    try {
                        parcelEventPublisher.publish(parcelId.toString(), ParcelEvent.POSTPONE);
                    } catch (Exception e) {
                        log.error("[session-service] [DeliveryAssignmentService.postponeParcel] Failed to publish POSTPONE event for FAILED assignment", e);
                    }

                    // Fetch parcel info
                    ParcelInfo parcel = null;
                    String receiverName = null;
                    try {
                        ParcelResponse parcelResponse = parcelServiceClient.fetchParcelResponse(parcelId.toString());
                        if (parcelResponse != null) {
                            parcel = parcelMapper.toParcelInfo(parcelResponse);
                            receiverName = parcel != null ? parcel.getReceiverName() : null;
                        }
                    } catch (Exception e) {
                        log.debug("[session-service] [DeliveryAssignmentService.postponeParcel] Failed to fetch parcel info: {}", e.getMessage());
                    }

                    return DeliveryAssignmentResponse.from(failedAssignment, parcel, failedAssignment.getSession(),
                            null, receiverName);
                }

                // No assignment found at all
                throw new ResourceNotFound(
                        "No IN_PROGRESS, DELAYED, or FAILED assignment found for parcel " + parcelId
                                + " and delivery man "
                                + deliveryManId);
            }
        } catch (Exception e) {
            log.error("[session-service] [DeliveryAssignmentService.postponeParcel] Error postponing parcel {} for delivery man {}", parcelId, deliveryManId, e);
            throw e;
        }
    }

    private DeliveryAssignmentResponse updateTaskState(UUID parcelId, UUID deliveryManId, RouteInfo routeInfo,
            AssignmentStatus newStatus, ParcelEvent parcelEvent, String failReason) {

        log.debug("Updating task state: parcelId={}, deliveryManId={}, newStatus={}, parcelEvent={}",
                parcelId, deliveryManId, newStatus, parcelEvent);

        // 1. Tìm assignment IN_PROGRESS trực tiếp theo parcelId và deliveryManId (không
        // cần active session)
        // Cho phép complete task ngay cả khi session đã completed/failed (có thể do
        // race condition)
        DeliveryAssignment assignment;
        try {
            assignment = deliveryAssignmentRepository
                    .findInProgressAssignmentByParcelIdAndDeliveryManId(parcelId.toString(), deliveryManId.toString())
                    .orElseThrow(() -> new ResourceNotFound(
                            "IN_PROGRESS assignment for parcel " + parcelId + " not found for delivery man "
                                    + deliveryManId));
            log.debug("Found assignment: {} for parcel: {} and delivery man: {}",
                    assignment.getId(), parcelId, deliveryManId);
        } catch (Exception e) {
            log.error("[session-service] [DeliveryAssignmentService.updateTaskState] Failed to find IN_PROGRESS assignment for parcel {} and delivery man {}", parcelId, deliveryManId, e);
            throw e;
        }

        // 2. Lấy session từ assignment (có thể không active nữa)
        DeliverySession session = assignment.getSession();
        if (session == null) {
            throw new ResourceNotFound("Assignment " + assignment.getId() + " has no associated session");
        }
        log.debug("Using session: {} (status: {}) for assignment: {}",
                session.getId(), session.getStatus(), assignment.getId());

        // 2.5. Block complete/fail if session is not IN_PROGRESS (session not started yet)
        if (session.getStatus() == SessionStatus.CREATED) {
            log.debug("[session-service] [DeliveryAssignmentService.updateTaskState] Session {} is CREATED (not started). Blocking action.", session.getId());
            throw new IllegalStateException("Phiên giao hàng chưa bắt đầu. Vui lòng bắt đầu phiên trước khi hoàn thành đơn hàng.");
        }

        // 3. Kiểm tra trạng thái
        try {
            ensureStatusIsProcessing(assignment);
            log.debug("Assignment {} status is valid: {}", assignment.getId(), assignment.getStatus());
        } catch (Exception e) {
            log.error("[session-service] [DeliveryAssignmentService.updateTaskState] Assignment {} status check failed. Current status: {}", assignment.getId(), assignment.getStatus(), e);
            throw e;
        }

        // 4. Cập nhật thông tin tuyến đường
        setRouteInfo(assignment, routeInfo);

        // 5. Đồng bộ với Parcel service
        // publish event to parcel-service instead of direct REST call
        try {
            log.debug("Publishing parcel event: {} for parcel: {}", parcelEvent, parcelId);
            parcelEventPublisher.publish(parcelId.toString(), parcelEvent);
            log.debug("Successfully published parcel event: {} for parcel: {}", parcelEvent, parcelId);
        } catch (Exception e) {
            log.error("[session-service] [DeliveryAssignmentService.updateTaskState] Failed to publish parcel status event for parcel {}", parcelId, e);
            // Don't throw - allow assignment status update to proceed even if event publish
            // fails
            // The event can be retried or handled separately
        }

        // 6. Cập nhật trạng thái (tham số hóa)
        assignment.setStatus(newStatus);
        assignment.setFailReason(failReason);

        // 7. Lưu
        try {
            deliveryAssignmentRepository.save(assignment);
            log.debug("Assignment {} updated to status: {}", assignment.getId(), newStatus);
        } catch (Exception e) {
            log.error("[session-service] [DeliveryAssignmentService.updateTaskState] Failed to save assignment {}", assignment.getId(), e);
            throw new RuntimeException("Failed to save assignment: " + e.getMessage(), e);
        }

        // 8. Fetch parcel information for response (includes receiver info from
        // UserSnapshot)
        ParcelInfo parcel = null;
        String receiverName = null;
        String receiverId = null;
        String receiverPhone = null;
        String parcelCode = null;
        try {
            log.debug("Fetching parcel information for parcelId: {}", parcelId);
            ParcelResponse parcelResponse = parcelServiceClient.fetchParcelResponse(parcelId.toString());
            if (parcelResponse != null) {
                parcel = parcelMapper.toParcelInfo(parcelResponse);
                // Extract receiver info from parcel response (already includes UserSnapshot
                // data)
                receiverName = parcel != null ? parcel.getReceiverName() : null;
                receiverId = parcel != null ? parcel.getReceiverId() : null;
                receiverPhone = parcel != null ? parcel.getReceiverPhoneNumber() : null;
                parcelCode = parcel != null ? parcel.getCode() : null;
                log.debug("Successfully fetched parcel information for parcelId: {}, receiverName: {}", parcelId,
                        receiverName);
            } else {
                log.debug("[session-service] [DeliveryAssignmentService.fetchParcelInfo] Parcel response is null for parcelId: {}", parcelId);
            }
        } catch (Exception e) {
            log.error("[session-service] [DeliveryAssignmentService.fetchParcelInfo] Failed to fetch parcel information for parcelId {}. Response will have null parcel info.", parcelId, e);
            // Don't throw - allow response to be created with null parcel info
            // This is a fallback to prevent the entire request from failing
        }

        // 9. Publish AssignmentCompletedEvent if task is completed (for notifications)
        if (newStatus == AssignmentStatus.COMPLETED && parcelEvent == ParcelEvent.DELIVERY_SUCCESSFUL) {
            try {
                AssignmentCompletedEvent completedEvent = AssignmentCompletedEvent.builder()
                        .eventId(java.util.UUID.randomUUID().toString())
                        .assignmentId(assignment.getId().toString())
                        .parcelId(parcelId.toString())
                        .parcelCode(parcelCode)
                        .sessionId(session.getId().toString())
                        .deliveryManId(deliveryManId.toString())
                        .deliveryManName(null) // Will be enriched by communication-service if needed
                        .receiverId(receiverId)
                        .receiverName(receiverName)
                        .receiverPhone(receiverPhone)
                        .completedAt(assignment.getUpdatedAt() != null ? assignment.getUpdatedAt()
                                : java.time.LocalDateTime.now())
                        .sourceService("session-service")
                        .createdAt(java.time.Instant.now())
                        .build();

                eventProducer.publishAssignmentCompleted(parcelId.toString(), completedEvent);
                log.debug("Published AssignmentCompletedEvent for parcel: {}, assignment: {}", parcelId,
                        assignment.getId());
            } catch (Exception e) {
                log.error("[session-service] [DeliveryAssignmentService.completeTask] Failed to publish AssignmentCompletedEvent for parcel {}. Continuing...", parcelId, e);
                // Don't throw - notification is not critical for task completion
            }
        }

        // // 10. Check if session should be auto-completed (no more pending tasks)
        // if (newStatus == AssignmentStatus.COMPLETED || newStatus == AssignmentStatus.FAILED) {
        //     checkAndAutoCompleteSession(session);
        // }

        // 11. Trả về DTO (receiverName comes from parcel which already includes
        // UserSnapshot data)
        // Note: deliveryManPhone is not needed - the shipper already knows their own
        // phone number
        return DeliveryAssignmentResponse.from(assignment, parcel, assignment.getSession(), null, receiverName);
    }

    /**
     * Check if session has no more pending tasks and auto-complete it
     */
    private void checkAndAutoCompleteSession(DeliverySession session) {
        if (session == null) {
            return;
        }
        
        // Only auto-complete sessions that are CREATED or IN_PROGRESS
        if (session.getStatus() != SessionStatus.CREATED && session.getStatus() != SessionStatus.IN_PROGRESS) {
            log.debug("[session-service] [DeliveryAssignmentService.checkAndAutoCompleteSession] Session {} is already {}. Skipping auto-complete check.", 
                session.getId(), session.getStatus());
            return;
        }
        
        // Count pending tasks (CREATED or IN_PROGRESS)
        long pendingCount = deliveryAssignmentRepository.countPendingTasksBySessionId(session.getId());
        log.debug("[session-service] [DeliveryAssignmentService.checkAndAutoCompleteSession] Session {} has {} pending tasks", 
            session.getId(), pendingCount);
        
        if (pendingCount == 0) {
            log.info("[session-service] [DeliveryAssignmentService.checkAndAutoCompleteSession] Session {} has no more pending tasks. Auto-completing session.", 
                session.getId());
            
            try {
                // Use SessionService.completeSession to properly publish events
                sessionService.completeSession(session.getId());
                log.info("[session-service] [DeliveryAssignmentService.checkAndAutoCompleteSession] Session {} auto-completed successfully.", 
                    session.getId());
            } catch (Exception e) {
                log.error("[session-service] [DeliveryAssignmentService.checkAndAutoCompleteSession] Failed to auto-complete session {}", 
                    session.getId(), e);
            }
        }
    }

    /**
     * getDailyTasks giờ sẽ trả về các task của SESSION ĐANG HOẠT ĐỘNG
     */
    @Override
    @Transactional(readOnly = true) // Dùng readOnly cho các hàm GET
    public PageResponse<DeliveryAssignmentResponse> getDailyTasks(
            UUID deliveryManId, List<String> status, int page, int size) {
        log.debug("Starting getDailyTasks for deliveryManId: {}, status: {}, page: {}, size: {}",
                deliveryManId, status, page, size);

        // 1. Xây dựng đối tượng phân trang
        // (Mặc định sắp xếp theo thời gian quét (scanedAt) mới nhất)
        Pageable pageable = PageUtil.build(page, size, "scanedAt", "desc", DeliveryAssignment.class);
        log.debug("Pageable created: page={}, size={}", page, size);

        // 2. Parse status strings to SessionStatus enum
        // Status parameter represents SESSION status (CREATED, IN_PROGRESS), not
        // assignment status
        List<SessionStatus> sessionStatuses = null;
        if (status != null && !status.isEmpty()) {
            try {
                sessionStatuses = status.stream()
                        .map(String::toUpperCase)
                        .map(SessionStatus::valueOf)
                        .collect(Collectors.toList());
                log.debug("Parsed session statuses: {}", sessionStatuses);
            } catch (IllegalArgumentException e) {
                log.debug("[session-service] [DeliveryAssignmentService.getDailyTasks] Invalid session status values provided: {}. Using default: CREATED, IN_PROGRESS", status);
                // Default to CREATED and IN_PROGRESS if invalid status provided
                sessionStatuses = List.of(SessionStatus.CREATED, SessionStatus.IN_PROGRESS);
            }
        } else {
            // Default: filter by CREATED and IN_PROGRESS sessions
            sessionStatuses = List.of(SessionStatus.CREATED, SessionStatus.IN_PROGRESS);
        }

        // 3. Xây dựng Specification (Tiêu chí lọc)
        Specification<DeliveryAssignment> spec = Specification
                .where(AssignmentSpecification.byDeliveryManId(deliveryManId))
                .and(AssignmentSpecification.bySessionStatusIn(sessionStatuses));
        log.debug("Specification created for filtering");

        // 4. Gọi Repository
        log.debug("Querying database for tasks...");
        Page<DeliveryAssignment> tasksPage = deliveryAssignmentRepository.findAll(spec, pageable);
        log.debug("Found {} tasks from database (total: {})", tasksPage.getNumberOfElements(),
                tasksPage.getTotalElements());

        // 5. Ánh xạ kết quả sang DTO
        log.debug("Enriching tasks with parcel information...");
        PageResponse<DeliveryAssignmentResponse> response = getEnrichedTasks(tasksPage);
        log.debug("Returning {} enriched tasks", response.getContent().size());
        return response;
    }

    /**
     * Lấy các task của một session cụ thể theo sessionId (phân trang).
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeliveryAssignmentResponse> getTasksBySessionId(
            UUID sessionId, int page, int size) {
        log.debug("Starting getTasksBySessionId for sessionId: {}, page: {}, size: {}", sessionId, page, size);

        // 1. Xây dựng đối tượng phân trang
        Pageable pageable = PageUtil.build(page, size, "scanedAt", "desc", DeliveryAssignment.class);

        // 2. Xây dựng Specification - filter theo sessionId
        Specification<DeliveryAssignment> spec = Specification
                .where(AssignmentSpecification.bySessionId(sessionId));

        // 3. Gọi Repository
        log.debug("Querying database for tasks in session {}...", sessionId);
        Page<DeliveryAssignment> tasksPage = deliveryAssignmentRepository.findAll(spec, pageable);
        log.debug("Found {} tasks from database (total: {})", tasksPage.getNumberOfElements(),
                tasksPage.getTotalElements());

        // 4. Ánh xạ kết quả sang DTO
        log.debug("Enriching tasks with parcel information...");
        PageResponse<DeliveryAssignmentResponse> response = getEnrichedTasks(tasksPage);
        log.debug("Returning {} enriched tasks", response.getContent().size());
        return response;
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
            int page, int size) {
        // 1. Phân trang
        Pageable pageable = PageUtil.build(page, size, "scanedAt", "desc", DeliveryAssignment.class);

        // 2. Xây dựng Specification
        // Lọc các phiên đã KẾT THÚC (COMPLETED hoặc FAILED)
        List<SessionStatus> terminalStatuses = Arrays.asList(SessionStatus.COMPLETED, SessionStatus.FAILED);

        Specification<DeliveryAssignment> spec = Specification
                .where(AssignmentSpecification.byDeliveryManId(deliveryManId))
                // .and(AssignmentSpecification.bySessionStatusIn(terminalStatuses)) // Lọc theo
                // trạng thái Session
                .and(AssignmentSpecification.hasAssignmentStatusIn(status)) // Lọc theo trạng thái Task
                .and(AssignmentSpecification.isCreatedAtBetween(createdAtStart, createdAtEnd)); // Lọc theo ngày tạo
                                                                                                // task
        // .and(AssignmentSpecification.isCompletedAtBetween(completedAtStart,
        // completedAtEnd)); // Lọc theo ngày hoàn thành

        // 3. Gọi Repository
        Page<DeliveryAssignment> tasksPage = deliveryAssignmentRepository.findAll(spec, pageable);
        log.debug("Found {} tasks matching criteria", tasksPage.getTotalElements());
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

        // 2. Fetch parcel information from parcel-service with error handling
        Map<String, ParcelResponse> parcelResponseMap = Collections.emptyMap();
        if (parcelIds.isEmpty()) {
            log.debug("[session-service] [DeliveryAssignmentService.getEnrichedTasks] No parcel IDs to fetch from parcel-service");
        } else {
            log.debug("Fetching {} parcels from parcel-service...", parcelIds.size());
            long startTime = System.currentTimeMillis();
            try {
                parcelResponseMap = parcelServiceClient.fetchParcelsBulk(parcelIds);
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Successfully fetched {} parcels from parcel-service in {}ms", parcelResponseMap.size(),
                        duration);
            } catch (Exception ex) {
                long duration = System.currentTimeMillis() - startTime;
                log.error(
                        "[session-service] [DeliveryAssignmentService.getEnrichedTasks] Failed to fetch parcels from parcel-service after {}ms. Error type: {}. Returning tasks without parcel details.",
                        duration, ex.getClass().getSimpleName(), ex);
                // Return empty response - tasks will be filtered out below
                // This prevents the entire request from failing when parcel-service is
                // unavailable
            }
        }

        Map<String, ParcelInfo> parcelInfoMap = parcelResponseMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> parcelMapper.toParcelInfo(entry.getValue())));

        // 3. Map dữ liệu - only include tasks where we successfully fetched parcel info
        // Note: Receiver info (name, phone) comes from parcel response which already
        // includes UserSnapshot data
        // Note: deliveryManPhone is not needed - the shipper already knows their own
        // phone number
        List<DeliveryAssignmentResponse> dtoList = tasks.stream().map(t -> {
            ParcelInfo parcelInfo = parcelInfoMap.get(t.getParcelId());
            if (parcelInfo == null) {
                log.debug("[session-service] [DeliveryAssignmentService.getEnrichedTasks] Parcel info not available for parcelId: {}. Skipping task.", t.getParcelId());
                return null;
            }
            // Extract receiver name from parcel info (already includes UserSnapshot data
            // from parcel-service)
            String receiverName = parcelInfo.getReceiverName();
            return DeliveryAssignmentResponse.from(t, parcelInfo, t.getSession(), null, receiverName);

        }).filter(response -> response != null)
                .toList();

        // Trả về PageResponse (giữ nguyên thông tin phân trang)
        return PageResponse.from(tasksPage, dtoList);
    }

    public Optional<ShipperInfo> getLatestDriverIdForParcel(String parcelId) {
        log.debug("Tìm kiếm tài xế gần nhất cho parcelId: {}", parcelId);

        Optional<DeliveryAssignment> latestAssignmentOpt = deliveryAssignmentRepository
                .findFirstByParcelIdOrderByUpdatedAtDesc(parcelId);

        if (latestAssignmentOpt.isEmpty()) {
            log.debug("[session-service] [DeliveryAssignmentService.getLatestDriverIdForParcel] Không tìm thấy assignment hợp lệ nào cho parcelId: {}", parcelId);
            return Optional.empty();
        }

        return latestAssignmentOpt.map(assignment -> {
            String driverId = assignment.getSession().getDeliveryManId();
            log.debug("Tìm thấy tài xế: {} cho parcelId: {}", driverId, parcelId);
            return new ShipperInfo(driverId, "Tài xế", "0912312312");
        });
    }

    @Override
    public Optional<UUID> getActiveAssignmentId(String parcelId, String deliveryManId) {
        log.debug("Tìm kiếm assignmentId cho parcelId: {} và deliveryManId: {}", parcelId, deliveryManId);

        // Validate UUID format
        try {
            UUID.fromString(parcelId);
            log.debug("[session-service] [DeliveryAssignmentService.getActiveAssignmentId] parcelId is valid UUID: {}", parcelId);
        } catch (IllegalArgumentException e) {
            log.error("[session-service] [DeliveryAssignmentService.getActiveAssignmentId] parcelId is not a valid UUID: {}", parcelId);
            return Optional.empty();
        }

        try {
            UUID.fromString(deliveryManId);
            log.debug("[session-service] [DeliveryAssignmentService.getActiveAssignmentId] deliveryManId is valid UUID: {}", deliveryManId);
        } catch (IllegalArgumentException e) {
            log.error("[session-service] [DeliveryAssignmentService.getActiveAssignmentId] deliveryManId is not a valid UUID: {}", deliveryManId);
            return Optional.empty();
        }

        // First, try to find active assignment (IN_PROGRESS in active session)
        Optional<DeliveryAssignment> assignmentOpt = deliveryAssignmentRepository
                .findActiveAssignmentByParcelIdAndDeliveryManId(parcelId, deliveryManId);

        if (assignmentOpt.isPresent()) {
            DeliveryAssignment assignment = assignmentOpt.get();
            UUID assignmentId = assignment.getId();
            log.debug(
                    "Tìm thấy active assignmentId: {} cho parcelId: {} và deliveryManId: {} (Session: {}, Status: {})",
                    assignmentId, parcelId, deliveryManId,
                    assignment.getSession() != null ? assignment.getSession().getId() : "null",
                    assignment.getStatus());
            return Optional.of(assignmentId);
        } else {
            log.debug("⚠️ Không tìm thấy active assignment (IN_PROGRESS in active session)");
        }

        // If not found, try to find IN_PROGRESS assignment (even if session is not
        // active)
        Optional<DeliveryAssignment> inProgressOpt = deliveryAssignmentRepository
                .findInProgressAssignmentByParcelIdAndDeliveryManId(parcelId, deliveryManId);

        if (inProgressOpt.isPresent()) {
            DeliveryAssignment assignment = inProgressOpt.get();
            UUID assignmentId = assignment.getId();
            log.debug(
                    "Tìm thấy IN_PROGRESS assignmentId: {} cho parcelId: {} và deliveryManId: {} (Session: {}, Status: {})",
                    assignmentId, parcelId, deliveryManId,
                    assignment.getSession() != null ? assignment.getSession().getId() : "null",
                    assignment.getStatus());
            return Optional.of(assignmentId);
        } else {
            log.debug("⚠️ Không tìm thấy IN_PROGRESS assignment (regardless of session status)");
        }

        // If still not found, try to find DELAYED assignment (for idempotent
        // operations)
        Optional<DeliveryAssignment> delayedOpt = deliveryAssignmentRepository
                .findFirstByParcelIdOrderByUpdatedAtDesc(parcelId)
                .filter(da -> da.getSession() != null && da.getSession().getDeliveryManId().equals(deliveryManId))
                .filter(da -> da.getStatus() == AssignmentStatus.DELAYED);

        if (delayedOpt.isPresent()) {
            DeliveryAssignment assignment = delayedOpt.get();
            UUID assignmentId = assignment.getId();
            log.debug(
                    "Tìm thấy DELAYED assignmentId: {} cho parcelId: {} và deliveryManId: {} (Session: {}, Status: {})",
                    assignmentId, parcelId, deliveryManId,
                    assignment.getSession() != null ? assignment.getSession().getId() : "null",
                    assignment.getStatus());
            return Optional.of(assignmentId);
        } else {
            log.debug("⚠️ Không tìm thấy DELAYED assignment");
        }

        // Debug: Check if parcel exists at all
        List<DeliveryAssignment> allAssignments = deliveryAssignmentRepository
                .findFirstByParcelIdOrderByUpdatedAtDesc(parcelId)
                .map(java.util.Collections::singletonList)
                .orElse(java.util.Collections.emptyList());
        log.debug("[session-service] [DeliveryAssignmentService.getActiveAssignmentId] Không tìm thấy assignment (active, IN_PROGRESS, hoặc DELAYED) cho parcelId: {} và deliveryManId: {}",
                parcelId, deliveryManId);
        log.debug("[session-service] [DeliveryAssignmentService.getActiveAssignmentId] Tổng số assignments cho parcelId {}: {}", parcelId, allAssignments.size());
        if (!allAssignments.isEmpty()) {
            log.debug("[session-service] [DeliveryAssignmentService.getActiveAssignmentId] Assignments found for parcelId {}:", parcelId);
            for (DeliveryAssignment da : allAssignments) {
                log.debug("[session-service] [DeliveryAssignmentService.getActiveAssignmentId]   - AssignmentId: {}, Status: {}, DeliveryManId: {}, SessionId: {}, SessionStatus: {}",
                        da.getId(),
                        da.getStatus(),
                        da.getSession() != null ? da.getSession().getDeliveryManId() : "null",
                        da.getSession() != null ? da.getSession().getId() : "null",
                        da.getSession() != null ? da.getSession().getStatus() : "null");
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<LatestAssignmentResponse> getLatestAssignmentForParcel(String parcelId) {
        return deliveryAssignmentRepository.findFirstByParcelIdOrderByUpdatedAtDesc(parcelId)
                .map(assignment -> LatestAssignmentResponse.builder()
                        .assignmentId(assignment.getId())
                        .sessionId(assignment.getSession() != null ? assignment.getSession().getId() : null)
                        .status(assignment.getStatus())
                        .deliveryManId(
                                assignment.getSession() != null ? assignment.getSession().getDeliveryManId() : null)
                        .build());
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse postponeByAssignmentId(UUID assignmentId,
            com.ds.session.session_service.common.entities.dto.request.PostponeAssignmentRequest request) {
        log.debug("Postponing assignment {} with reason: {}, postponeDateTime: {}, moveToEnd: {}",
                assignmentId, request.getReason(), request.getPostponeDateTime(), request.getMoveToEnd());

        // 1. Find assignment
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFound("Assignment not found: " + assignmentId));

        // 2. Verify assignment is in active session
        DeliverySession session = assignment.getSession();
        if (session.getStatus() != SessionStatus.IN_PROGRESS && session.getStatus() != SessionStatus.CREATED) {
            throw new IllegalStateException("Assignment " + assignmentId
                    + " is not in an active session. Session status: " + session.getStatus());
        }

        // 3. Verify assignment status
        if (assignment.getStatus() != AssignmentStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Assignment " + assignmentId + " is not IN_PROGRESS. Current status: " + assignment.getStatus());
        }

        // 4. Update route info if provided
        if (request.getRouteInfo() != null) {
            setRouteInfo(assignment, request.getRouteInfo());
        }

        // 5. Check if postpone datetime is outside session time
        boolean isPostponeOutsideSession = false;
        if (request.getPostponeDateTime() != null) {
            isPostponeOutsideSession = isPostponeOutsideSessionTime(session, request.getPostponeDateTime());
            log.debug("Postpone datetime check: {} is {} session time",
                    request.getPostponeDateTime(),
                    isPostponeOutsideSession ? "OUTSIDE" : "WITHIN");
        }

        // 6. Handle postpone logic based on whether it's outside session time
        boolean shouldMoveToEnd = false;
        if (!isPostponeOutsideSession && Boolean.TRUE.equals(request.getMoveToEnd())) {
            // Postpone is within session time and moveToEnd is true -> move to end
            shouldMoveToEnd = true;
            log.debug("Postpone is within session time and moveToEnd=true. Will move parcel to end of route.");
        } else if (isPostponeOutsideSession) {
            // Postpone is outside session time -> DELAY
            log.debug("Postpone is outside session time. Will set assignment to DELAYED.");
        } else {
            // Default: DELAY
            log.debug("Default behavior: Will set assignment to DELAYED.");
        }

        // 7. Update assignment status
        if (shouldMoveToEnd) {
            // Keep assignment as IN_PROGRESS but mark it to be moved to end (zone service
            // will handle this)
            // We'll add a flag or note in failReason to indicate this
            assignment.setFailReason(request.getReason() + " [MOVE_TO_END]");
            log.debug("Assignment {} will be moved to end of route (zone service will handle)", assignmentId);
            // Note: Zone service needs to handle the moveToEnd flag when recalculating
            // route
        } else {
            // Set to FAILED (when client accepts postpone request)
            assignment.setStatus(AssignmentStatus.FAILED);
            assignment.setFailReason(request.getReason());

            // Publish parcel event to change parcel status to DELAY
            try {
                log.debug("Publishing parcel event: POSTPONE for parcel: {} (assignment set to FAILED)", assignment.getParcelId());
                parcelEventPublisher.publish(assignment.getParcelId(), ParcelEvent.POSTPONE);
                log.debug("Successfully published parcel event: POSTPONE for parcel: {}", assignment.getParcelId());
            } catch (Exception e) {
                log.error("Failed to publish parcel status event for parcel {}: {}", assignment.getParcelId(),
                        e.getMessage(), e);
            }
        }

        // 8. Save assignment
        deliveryAssignmentRepository.save(assignment);
        log.debug("Assignment {} updated", assignmentId);

        // // 9. Check if session should be auto-completed
        // if (isPostponeOutsideSession) {
        //     checkAndAutoCompleteSessionIfNeeded(session);
        // } else if (shouldMoveToEnd) {
        //     // If moving to end, also check if only postponed parcels remain
        //     checkAndAutoCompleteSessionIfOnlyPostponedRemain(session);
        // }

        // 10. Fetch parcel information (includes receiver info from UserSnapshot)
        ParcelInfo parcel = null;
        String receiverName = null;
        try {
            if (!shouldMoveToEnd) {
                // Only update parcel status if not moving to end
                ParcelResponse parcelResponse = parcelServiceClient.changeParcelStatus(assignment.getParcelId(),
                        ParcelEvent.POSTPONE);
                if (parcelResponse != null) {
                    parcel = parcelMapper.toParcelInfo(parcelResponse);
                    receiverName = parcel != null ? parcel.getReceiverName() : null;
                }
            } else {
                // If moving to end, just fetch parcel info without changing status
                ParcelResponse parcelResponse = parcelServiceClient.fetchParcelResponse(assignment.getParcelId());
                if (parcelResponse != null) {
                    parcel = parcelMapper.toParcelInfo(parcelResponse);
                    receiverName = parcel != null ? parcel.getReceiverName() : null;
                }
            }
        } catch (Exception e) {
            log.debug("[session-service] [DeliveryAssignmentService.sendPostponeNotification] Failed to fetch parcel info for parcel {}: {}", assignment.getParcelId(), e.getMessage());
        }

        // 11. Send notification if parcel is postponed (out of session)
        if (isPostponeOutsideSession && !shouldMoveToEnd) {
            sendPostponeNotification(assignment, session, request.getPostponeDateTime());
        }

        // 12. Return DTO
        return DeliveryAssignmentResponse.from(assignment, parcel, session, null, receiverName);
    }

    /**
     * Check if postpone datetime is outside session time
     * Session time: startTime + 13 hours OR before 21:00 same day
     */
    private boolean isPostponeOutsideSessionTime(DeliverySession session, LocalDateTime postponeDateTime) {
        if (session.getStartTime() == null) {
            return true; // No start time means session is invalid
        }

        LocalDateTime sessionStart = session.getStartTime();
        LocalDateTime sessionEnd = sessionStart.plusHours(13);
        LocalDateTime sessionEndByTime = sessionStart.toLocalDate().atTime(LocalTime.of(21, 0));
        LocalDateTime actualSessionEnd = sessionEnd.isBefore(sessionEndByTime) ? sessionEnd : sessionEndByTime;

        return postponeDateTime.isAfter(actualSessionEnd);
    }

    /**
     * Check if session should be auto-completed when postpone is outside session
     * time
     * Auto-complete if no more IN_PROGRESS assignments remain
     */
    private void checkAndAutoCompleteSessionIfNeeded(DeliverySession session) {
        long remainingInProgress = deliveryAssignmentRepository.countBySession_IdAndStatus(
                session.getId(), AssignmentStatus.IN_PROGRESS);

        log.debug("Checking if session {} should be auto-completed. Remaining IN_PROGRESS: {}",
                session.getId(), remainingInProgress);

        if (remainingInProgress == 0) {
            log.debug("No more IN_PROGRESS assignments in session {}. Auto-completing session.", session.getId());
            try {
                sessionService.completeSession(session.getId());
                log.debug("Session {} auto-completed successfully", session.getId());
            } catch (Exception e) {
                log.error("Failed to auto-complete session {}: {}", session.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Check if session should be auto-completed when only postponed parcels remain
     * Auto-complete session and set all postponed parcels to DELAYED
     */
    private void checkAndAutoCompleteSessionIfOnlyPostponedRemain(DeliverySession session) {
        long remainingInProgress = deliveryAssignmentRepository.countBySession_IdAndStatus(
                session.getId(), AssignmentStatus.IN_PROGRESS);
        long delayedCount = deliveryAssignmentRepository.countBySession_IdAndStatus(
                session.getId(), AssignmentStatus.DELAYED);

        log.debug(
                "Checking if session {} should be auto-completed (only postponed remain). IN_PROGRESS: {}, DELAYED: {}",
                session.getId(), remainingInProgress, delayedCount);

        // If no IN_PROGRESS and has DELAYED, auto-complete
        if (remainingInProgress == 0 && delayedCount > 0) {
            log.debug("Only postponed assignments remain in session {}. Auto-completing session.", session.getId());
            try {
                sessionService.completeSession(session.getId());
                log.debug("Session {} auto-completed successfully (only postponed remain)", session.getId());
            } catch (Exception e) {
                log.error("Failed to auto-complete session {}: {}", session.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Send notification to user when parcel is postponed (out of session)
     */
    private void sendPostponeNotification(DeliveryAssignment assignment, DeliverySession session,
            LocalDateTime postponeDateTime) {
        try {
            // Fetch parcel info to get receiver details
            ParcelResponse parcelResponse = parcelServiceClient.fetchParcelResponse(assignment.getParcelId());
            if (parcelResponse == null) {
                log.debug("[session-service] [DeliveryAssignmentService.sendPostponeNotification] Cannot send postpone notification: parcel {} not found", assignment.getParcelId());
                return;
            }

            ParcelInfo parcel = parcelMapper.toParcelInfo(parcelResponse);

            // Create and publish ParcelPostponedEvent
            com.ds.session.session_service.common.entities.dto.event.ParcelPostponedEvent postponedEvent = com.ds.session.session_service.common.entities.dto.event.ParcelPostponedEvent
                    .builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .assignmentId(assignment.getId().toString())
                    .parcelId(assignment.getParcelId())
                    .parcelCode(parcel != null ? parcel.getCode() : null)
                    .sessionId(session.getId().toString())
                    .deliveryManId(session.getDeliveryManId())
                    .receiverId(parcel != null ? parcel.getReceiverId() : null)
                    .receiverName(parcel != null ? parcel.getReceiverName() : null)
                    .receiverPhone(parcel != null ? parcel.getReceiverPhoneNumber() : null)
                    .postponeDateTime(postponeDateTime)
                    .reason(assignment.getFailReason())
                    .createdAt(java.time.Instant.now())
                    .sourceService("session-service")
                    .build();

            eventProducer.publishParcelPostponed(assignment.getParcelId(), postponedEvent);
            log.debug("Published ParcelPostponedEvent for parcel: {}, assignment: {}",
                    assignment.getParcelId(), assignment.getId());
        } catch (Exception e) {
            log.error("Failed to send postpone notification for parcel {}: {}",
                    assignment.getParcelId(), e.getMessage(), e);
        }
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
        if (routeInfo == null)
            return;
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
                .orElseThrow(() -> new ResourceNotFound(
                        "Assignment for parcel " + parcelId + " not found in session " + sessionId));
    }

    private void ensureStatusIsProcessing(DeliveryAssignment assignment) {
        if (!AssignmentStatus.IN_PROGRESS.equals(assignment.getStatus())) {
            throw new IllegalStateException("Can not finish assignment that is not currently IN_PROGRESS.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeliveryAssignmentResponse> getDailyTasksV0(
            com.ds.session.session_service.common.entities.dto.request.PagingRequestV0 request) {
        // V0: Simple paging with sorting only, no dynamic filters
        Pageable pageable = PageUtil.build(
                request.getPage(),
                request.getSize(),
                "scanedAt",
                "DESC",
                DeliveryAssignment.class);

        Page<DeliveryAssignment> tasksPage = deliveryAssignmentRepository.findAll(pageable);
        return getEnrichedTasks(tasksPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeliveryAssignmentResponse> getDailyTasksV2(
            com.ds.session.session_service.common.entities.dto.request.PagingRequestV2 request) {
        // V2: Enhanced filtering with operations between each pair
        Specification<DeliveryAssignment> spec = Specification.where(null);

        if (request.getFiltersOrNull() != null) {
            spec = com.ds.session.session_service.common.utils.EnhancedQueryParserV2.parseFilterGroup(
                    request.getFiltersOrNull(),
                    DeliveryAssignment.class);
        }

        Pageable pageable = PageUtil.build(
                request.getPage(),
                request.getSize(),
                "scanedAt",
                "DESC",
                DeliveryAssignment.class);

        Page<DeliveryAssignment> tasksPage = deliveryAssignmentRepository.findAll(spec, pageable);
        return getEnrichedTasks(tasksPage);
    }

    @Override
    public DeliveryAssignmentResponse updateAssignmentStatus(UUID sessionId, UUID assignmentId,
            UpdateAssignmentStatusRequest request) {
        log.debug("[session-service] [DeliveryAssignmentService.updateAssignmentStatus] Updating assignment {} status in session {} to {}", assignmentId, sessionId,
                request.getAssignmentStatus());

        // 1. Find assignment in session
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFound("Assignment not found: " + assignmentId));

        // 2. Verify assignment belongs to session
        if (!assignment.getSession().getId().equals(sessionId)) {
            throw new IllegalStateException("Assignment " + assignmentId + " does not belong to session " + sessionId);
        }

        // 3. Update route info if provided
        if (request.getRouteInfo() != null) {
            setRouteInfo(assignment, request.getRouteInfo());
        }

        // 4. Update parcel status via Parcel service
        ParcelInfo parcel = updateParcelStatusAndMap(UUID.fromString(assignment.getParcelId()),
                request.getParcelEvent());

        // 5. Update assignment status
        assignment.setStatus(request.getAssignmentStatus());
        if (request.getFailReason() != null) {
            assignment.setFailReason(request.getFailReason());
        }

        // 6. Save
        deliveryAssignmentRepository.save(assignment);

        // 7. Extract receiver info from parcel (already includes UserSnapshot data from
        // parcel-service)
        String receiverName = parcel != null ? parcel.getReceiverName() : null;

        // 8. Return DTO (deliveryManPhone not needed - shipper knows their own phone)
        return DeliveryAssignmentResponse.from(assignment, parcel, assignment.getSession(), null, receiverName);
    }

    private ParcelInfo updateParcelStatusAndMap(UUID parcelId, ParcelEvent event) {
        // If you still want synchronous confirmation, you can optionally call
        // parcelServiceClient here as fallback
        try {
            ParcelResponse response = parcelServiceClient.changeParcelStatus(parcelId.toString(), event);
            log.debug("[session-service] [DeliveryAssignmentService.updateParcelStatusAndMap] parcel status (sync fallback): {}, event: {}", response.getStatus(), event);
            return parcelMapper.toParcelInfo(response);
        } catch (Exception ex) {
            log.debug("[session-service] [DeliveryAssignmentService.updateParcelStatusAndMap] Parcel service sync fallback failed for parcel {}: {}. Returning null for parcel info.", parcelId,
                    ex.getMessage());
            return null;
        }
    }

    /**
     * Save delivery confirmation point with location
     */
    private void saveConfirmationPoint(
            DeliveryAssignment assignment,
            Double lat,
            Double lon,
            java.time.LocalDateTime timestamp,
            String confirmationType) {
        try {
            // Calculate distance from parcel (if parcel info available)
            Double distanceFromParcel = null;
            try {
                ParcelResponse parcelResponse = parcelServiceClient.fetchParcelResponse(assignment.getParcelId());
                if (parcelResponse != null && parcelResponse.getLat() != null && parcelResponse.getLon() != null) {
                    distanceFromParcel = calculateDistance(
                        lat, lon,
                        parcelResponse.getLat().doubleValue(), parcelResponse.getLon().doubleValue()
                    );
                }
            } catch (Exception e) {
                log.debug("[session-service] [DeliveryAssignmentService.saveConfirmationPoint] Failed to calculate distance from parcel: {}", e.getMessage());
            }

            com.ds.session.session_service.app_context.models.DeliveryConfirmationPoint confirmationPoint = 
                com.ds.session.session_service.app_context.models.DeliveryConfirmationPoint.builder()
                    .assignmentId(assignment.getId())
                    .sessionId(assignment.getSession().getId().toString())
                    .latitude(lat)
                    .longitude(lon)
                    .confirmedAt(timestamp != null ? timestamp : java.time.LocalDateTime.now())
                    .confirmationType(confirmationType)
                    .distanceFromParcel(distanceFromParcel)
                    .build();

            confirmationPointRepository.save(confirmationPoint);
            log.debug("[session-service] [DeliveryAssignmentService.saveConfirmationPoint] Saved confirmation point for assignment {}", assignment.getId());
        } catch (Exception e) {
            log.error("[session-service] [DeliveryAssignmentService.saveConfirmationPoint] Failed to save confirmation point", e);
            // Don't throw - confirmation point is not critical for task completion
        }
    }

    /**
     * Calculate distance between two points using Haversine formula (in meters)
     */
    private Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        final int R = 6371000; // Earth radius in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
