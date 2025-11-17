package com.ds.session.session_service.business.v1.services;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.app_context.models.DeliverySession;
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentRepository;
import com.ds.session.session_service.app_context.repositories.DeliverySessionRepository;
import com.ds.session.session_service.application.client.parcelclient.ParcelServiceClient; 
import com.ds.session.session_service.application.client.parcelclient.response.ParcelResponse;
import com.ds.session.session_service.application.client.userclient.UserServiceClient;
import com.ds.session.session_service.application.client.userclient.response.DeliveryManResponse;
import com.ds.session.session_service.application.client.zoneclient.ZoneServiceClient;
import com.ds.session.session_service.application.client.zoneclient.request.RouteRequest;
import com.ds.session.session_service.application.client.zoneclient.response.RouteResponse;
import com.ds.session.session_service.common.entities.dto.common.PagedData;
import com.ds.session.session_service.common.entities.dto.request.CreateSessionRequest;
import com.ds.session.session_service.common.entities.dto.request.PagingRequestV2;
import com.ds.session.session_service.common.entities.dto.response.AssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.SessionResponse;
import com.ds.session.session_service.common.entities.dto.sort.SortConfig;
import com.ds.session.session_service.common.enums.AssignmentStatus;
import com.ds.session.session_service.common.enums.ParcelEvent;
import com.ds.session.session_service.common.enums.SessionStatus;
import com.ds.session.session_service.common.exceptions.ResourceNotFound;
import com.ds.session.session_service.common.interfaces.ISessionService;
import com.ds.session.session_service.common.utils.EnhancedQueryParserV2;
import com.ds.session.session_service.infrastructure.kafka.ParcelEventPublisher;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService implements ISessionService {

    private final DeliverySessionRepository sessionRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final ParcelServiceClient parcelApiClient; 
    private final ParcelEventPublisher parcelEventPublisher;
    private final UserServiceClient userServiceClient;
    private final ZoneServiceClient zoneServiceClient;
    
    // Session expiration: 13 hours or before 21:00 on the same day
    private static final int SESSION_MAX_HOURS = 13;
    private static final LocalTime SESSION_END_TIME = LocalTime.of(21, 0);
    private static final int MAX_DELIVERY_HOURS = 5; // Maximum 5 hours for delivery routing 


    @Override
    @Transactional
    public AssignmentResponse acceptParcelToSession(String deliveryManId, String parcelId) {
        long startTime = System.currentTimeMillis();
        log.info("Processing scan for parcel {} by shipper {}", parcelId, deliveryManId);

        // 1. Kiểm tra trạng thái đơn hàng từ ParcelService
        long fetchStart = System.currentTimeMillis();
        ParcelResponse parcelInfo = parcelApiClient.fetchParcelResponse(parcelId);
        log.debug("Fetched parcel info in {}ms", System.currentTimeMillis() - fetchStart);
        if (parcelInfo == null || !"IN_WAREHOUSE".equals(parcelInfo.getStatus())) {
            throw new IllegalStateException("Parcel " + parcelId + " is not IN_WAREHOUSE or does not exist. Current status: " + (parcelInfo != null ? parcelInfo.getStatus() : "N/A"));
        }

        // 2. Kiểm tra xem đơn hàng này đã thuộc một phiên (session) ĐANG HOẠT ĐỘNG (CREATED hoặc IN_PROGRESS)
        // của BẤT KỲ shipper nào khác chưa.
        Optional<DeliveryAssignment> existingInProgressAssignment = assignmentRepository.findActiveByParcelId(parcelId, SessionStatus.IN_PROGRESS);
        Optional<DeliveryAssignment> existingCreatedAssignment = assignmentRepository.findActiveByParcelId(parcelId, SessionStatus.CREATED);
        
        Optional<DeliveryAssignment> existingActiveAssignment = existingInProgressAssignment.isPresent() 
            ? existingInProgressAssignment 
            : existingCreatedAssignment;
            
        if (existingActiveAssignment.isPresent()) {
            DeliveryAssignment assignment = existingActiveAssignment.get();
            // Nếu đơn hàng đã được gán cho chính shipper này (quét lại)
            if (assignment.getSession().getDeliveryManId().equals(deliveryManId)) {
                log.warn("Parcel {} already in this active session ({}). Returning existing task.", 
                    parcelId, assignment.getSession().getStatus());
                return toAssignmentResponse(assignment);
            }
            // Nếu đơn hàng bị gán cho shipper khác
            throw new IllegalStateException("Parcel " + parcelId + " is already in an active session (status: " 
                + assignment.getSession().getStatus() + ") of another delivery man: " 
                + assignment.getSession().getDeliveryManId());
        }
        
        // 3. Tìm phiên (session) đang CREATED hoặc IN_PROGRESS của shipper này
        Optional<DeliverySession> createdSessionOpt = sessionRepository.findByDeliveryManIdAndStatus(deliveryManId, SessionStatus.CREATED);
        Optional<DeliverySession> inProgressSessionOpt = sessionRepository.findByDeliveryManIdAndStatus(deliveryManId, SessionStatus.IN_PROGRESS);

        DeliverySession sessionToUse;

        if (inProgressSessionOpt.isPresent()) {
            // Ưu tiên phiên IN_PROGRESS
            log.info("Found IN_PROGRESS session {} for shipper {}. Adding task.", inProgressSessionOpt.get().getId(), deliveryManId);
            sessionToUse = inProgressSessionOpt.get();
        } else if (createdSessionOpt.isPresent()) {
            // Nếu có phiên CREATED, sử dụng nó
            log.info("Found CREATED session {} for shipper {}. Adding task.", createdSessionOpt.get().getId(), deliveryManId);
            sessionToUse = createdSessionOpt.get();
        } else {
            // 4a. Logic: "scan đơn đầu tiên sẽ tạo phiên ở trạng thái CREATED"
            log.info("No active session found for shipper {}. Creating a new CREATED session.", deliveryManId);
            sessionToUse = DeliverySession.builder()
                .deliveryManId(deliveryManId)
                .status(SessionStatus.CREATED)
                .startTime(LocalDateTime.now())
                .build();
        }

        // 5. Tạo Assignment (Task) mới
        DeliveryAssignment newAssignment = DeliveryAssignment.builder()
            .parcelId(parcelId)
            .status(AssignmentStatus.IN_PROGRESS)
            .scanedAt(LocalDateTime.now())
            .build();
        
        // 6. Kiểm tra hết hạn session trước khi thêm đơn
        if (isSessionExpired(sessionToUse)) {
            log.warn("Session {} is expired. Auto-failing session before adding parcel.", sessionToUse.getId());
            failSession(sessionToUse.getId(), "Phiên giao hết hạn");
            throw new IllegalStateException("Session has expired. Please create a new session.");
        }

        // 7. Validation: Nếu số đơn > 5, kiểm tra routing time
        int currentParcelCount = sessionToUse.getAssignments().size();
        if (currentParcelCount >= 5) {
            log.info("Session {} has {} parcels. Validating routing time before adding new parcel.", 
                sessionToUse.getId(), currentParcelCount);
            
            // Validate routing time
            long routingStart = System.currentTimeMillis();
            validateRoutingTime(sessionToUse, parcelInfo);
            log.info("Routing validation completed in {}ms", System.currentTimeMillis() - routingStart);
        }

        // 8. Liên kết task vào session
        sessionToUse.addAssignment(newAssignment);

        // 9. Gọi Parcel-Service để cập nhật trạng thái đơn hàng
        try {
            parcelEventPublisher.publish(parcelId, ParcelEvent.SCAN_QR);
        } catch (Exception e) {
            log.error("Failed to publish parcel status event for parcel {}: {}", parcelId, e.getMessage());
            throw new RuntimeException("Failed to publish parcel status event: " + e.getMessage(), e);
        }

        // 10. Lưu session (và task mới sẽ được lưu theo nhờ CascadeType.ALL)
        sessionRepository.save(sessionToUse);
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Successfully added parcel {} to session {} in {}ms.", parcelId, sessionToUse.getId(), totalTime);

        // Trả về task vừa được tạo
        return toAssignmentResponse(newAssignment);
    }


    @Override
    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        log.info("Creating new session for delivery man: {}", request.getDeliveryManId());

        // 1. Kiểm tra xem shipper đã có phiên IN_PROGRESS hoặc CREATED nào chưa
        sessionRepository.findByDeliveryManIdAndStatus(request.getDeliveryManId(), SessionStatus.IN_PROGRESS)
            .ifPresent(session -> {
                throw new IllegalStateException("Delivery man " + request.getDeliveryManId() + " already has an IN_PROGRESS session.");
            });
        sessionRepository.findByDeliveryManIdAndStatus(request.getDeliveryManId(), SessionStatus.CREATED)
            .ifPresent(session -> {
                throw new IllegalStateException("Delivery man " + request.getDeliveryManId() + " already has a CREATED session.");
            });
            
        // 2. Tạo Session cha với trạng thái IN_PROGRESS (batch creation goes directly to IN_PROGRESS)
        DeliverySession session = DeliverySession.builder()
            .deliveryManId(request.getDeliveryManId())
            .status(SessionStatus.IN_PROGRESS)
            .startTime(LocalDateTime.now())
            .build();

        // 3. Tạo các Assignment (Task) con
        for (String parcelId : request.getParcelIds()) {
            DeliveryAssignment assignment = DeliveryAssignment.builder()
                .parcelId(parcelId)
                .status(AssignmentStatus.IN_PROGRESS)
                .scanedAt(LocalDateTime.now())
                .build();
            
            // Dùng hàm helper để liên kết 2 chiều
            session.addAssignment(assignment);

            // 4. TODO: Báo cho Parcel-Service biết đơn hàng này đã ON_ROUTE
            try {
                parcelEventPublisher.publish(parcelId, ParcelEvent.SCAN_QR);
            } catch (Exception e) {
                log.error("Failed to publish parcel status event for parcel {}: {}", parcelId, e.getMessage());
                throw new RuntimeException("Failed to publish parcel status event: " + e.getMessage(), e);
            }
        }
        
        // 5. Lưu Session (và các Assignment con nhờ CascadeType.ALL)
        DeliverySession savedSession = sessionRepository.save(session);
        log.info("Session {} created with {} tasks.", savedSession.getId(), savedSession.getAssignments().size());

        return toSessionResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionResponse createSessionPrepared(String deliveryManId) {
        log.info("Creating prepared session (CREATED status) for delivery man: {}", deliveryManId);

        // 1. Kiểm tra xem shipper đã có phiên CREATED hoặc IN_PROGRESS nào chưa
        sessionRepository.findByDeliveryManIdAndStatus(deliveryManId, SessionStatus.CREATED)
            .ifPresent(session -> {
                throw new IllegalStateException("Delivery man " + deliveryManId + " already has a CREATED session.");
            });
        sessionRepository.findByDeliveryManIdAndStatus(deliveryManId, SessionStatus.IN_PROGRESS)
            .ifPresent(session -> {
                throw new IllegalStateException("Delivery man " + deliveryManId + " already has an IN_PROGRESS session.");
            });

        // 2. Tạo Session ở trạng thái CREATED
        DeliverySession session = DeliverySession.builder()
            .deliveryManId(deliveryManId)
            .status(SessionStatus.CREATED)
            .startTime(LocalDateTime.now())
            .build();

        DeliverySession savedSession = sessionRepository.save(session);
        log.info("Prepared session {} created for delivery man {}.", savedSession.getId(), deliveryManId);

        return toSessionResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionResponse startSession(UUID sessionId) {
        log.info("Starting session {} (CREATED -> IN_PROGRESS)", sessionId);
        DeliverySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFound("Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.CREATED) {
            throw new IllegalStateException("Session " + sessionId + " must be in CREATED status to start. Current status: " + session.getStatus());
        }

        session.setStatus(SessionStatus.IN_PROGRESS);
        DeliverySession savedSession = sessionRepository.save(session);
        
        // Update all parcels in the session to ON_ROUTE status
        List<DeliveryAssignment> assignments = savedSession.getAssignments().stream()
            .filter(a -> a.getStatus() == AssignmentStatus.IN_PROGRESS)
            .toList();
        
        log.info("Session {} started. Updating {} parcels to ON_ROUTE status", sessionId, assignments.size());
        
        for (DeliveryAssignment assignment : assignments) {
            try {
                parcelEventPublisher.publish(assignment.getParcelId(), ParcelEvent.SCAN_QR);
                log.debug("Published SCAN_QR event for parcel {} to update to ON_ROUTE", assignment.getParcelId());
            } catch (Exception e) {
                log.error("Failed to publish SCAN_QR event for parcel {}: {}", assignment.getParcelId(), e.getMessage());
                // Don't throw - continue with other parcels
            }
        }
        
        log.info("Session {} started successfully. {} parcels updated to ON_ROUTE", sessionId, assignments.size());

        return toSessionResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionResponse completeSession(UUID sessionId) {
        log.info("Completing session {}", sessionId);
        DeliverySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFound("Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            log.warn("Session {} is not IN_PROGRESS (current: {}). Cannot complete.", sessionId, session.getStatus());
            throw new IllegalStateException("Session " + sessionId + " must be IN_PROGRESS to complete. Current status: " + session.getStatus());
        }
        
        // Kiểm tra lại (phòng hờ): Đảm bảo tất cả task đã xong
        long pendingTasks = assignmentRepository.countBySession_IdAndStatus(session.getId(), AssignmentStatus.IN_PROGRESS);
        if (pendingTasks > 0) {
            throw new IllegalStateException("Cannot complete session " + sessionId + ": " + pendingTasks + " tasks are still PENDING.");
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setEndTime(LocalDateTime.now());
        DeliverySession savedSession = sessionRepository.save(session);
        
        // Handle DELAYED tasks: notify Parcel-Service to change DELAYED parcels back to IN_WAREHOUSE
        // This includes:
        // 1. Tasks that were DELAYED due to customer request (POSTPONE) during delivery
        // 2. Tasks that were DELAYED due to session failure (now handled by failSession)
        List<DeliveryAssignment> delayedTasks = savedSession.getAssignments().stream()
            .filter(a -> a.getStatus().equals(AssignmentStatus.DELAYED))
            .toList();
        
        log.info("Session {} completed. Processing {} DELAYED tasks to return to IN_WAREHOUSE", sessionId, delayedTasks.size());
        
        for (DeliveryAssignment task : delayedTasks) {
            try {
                // END_SESSION event will change parcel from DELAYED to IN_WAREHOUSE
                parcelApiClient.changeParcelStatus(task.getParcelId(), ParcelEvent.END_SESSION);
                log.info("Parcel {} returned to IN_WAREHOUSE from DELAYED", task.getParcelId());
            } catch (Exception e) {
                log.error("Failed to call Parcel-Service for parcel {}: {}", task.getParcelId(), e.getMessage());
            }
        }
        
        return toSessionResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionResponse failSession(UUID sessionId, String reason) {
        log.warn("Failing session {} due to: {}", sessionId, reason);
        DeliverySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFound("Session not found: " + sessionId));
        
        // Allow failing CREATED or IN_PROGRESS sessions
        // Session fail is when: expired, shipper cancels, or admin cancels
        // NOT when individual parcels fail
        if (session.getStatus() != SessionStatus.IN_PROGRESS && session.getStatus() != SessionStatus.CREATED) {
            log.warn("Session {} is already in state {}. Cannot fail.", sessionId, session.getStatus());
            throw new IllegalStateException("Session " + sessionId + " must be in IN_PROGRESS or CREATED status to fail. Current status: " + session.getStatus());
        }
        
        session.setStatus(SessionStatus.FAILED);
        session.setEndTime(LocalDateTime.now());
        
        DeliverySession savedSession = sessionRepository.save(session);

        // When session fails (expired, cancelled by shipper/admin), all undelivered parcels (IN_PROGRESS) 
        // must be set to DELAYED status, not FAILED
        List<DeliveryAssignment> pendingTasks = assignmentRepository.findBySession_IdAndStatus(sessionId, AssignmentStatus.IN_PROGRESS);
        log.info("Session {} failed. Setting {} pending tasks to DELAYED status", sessionId, pendingTasks.size());
        
        for (DeliveryAssignment task : pendingTasks) {
            // Set assignment status to DELAYED (not FAILED)
            task.setStatus(AssignmentStatus.DELAYED);
            task.setFailReason("Session Failed: " + reason);
            assignmentRepository.save(task);

            // Notify Parcel-Service: parcel should be delayed (POSTPONE event)
            // This will change parcel status from ON_ROUTE to DELAYED
            try {
                parcelApiClient.changeParcelStatus(task.getParcelId(), ParcelEvent.POSTPONE);
                log.info("Parcel {} set to DELAYED due to session failure", task.getParcelId());
            } catch (Exception e) {
                log.error("Failed to call Parcel-Service for parcel {}: {}", task.getParcelId(), e.getMessage());
            }
        }
        
        return toSessionResponse(savedSession);
    }

    @Override
    public SessionResponse getSessionById(UUID sessionId) {
        DeliverySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));
        
        // Check if session is expired and auto-fail it
        if (isSessionExpired(session)) {
            log.warn("Session {} is expired. Auto-failing session.", sessionId);
            failSession(sessionId, "Phiên giao hết hạn");
            // Re-fetch the session after failing
            session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));
        }
        
        return toSessionResponse(session);
    }
    
    @Override
    public SessionResponse getActiveSession(String deliveryManId) {
        log.info("Getting active session for delivery man: {}", deliveryManId);
        
        // Try IN_PROGRESS first, then CREATED
        Optional<DeliverySession> inProgressSession = sessionRepository.findByDeliveryManIdAndStatus(deliveryManId, SessionStatus.IN_PROGRESS);
        if (inProgressSession.isPresent()) {
            DeliverySession session = inProgressSession.get();
            // Check if session is expired and auto-fail it
            if (isSessionExpired(session)) {
                log.warn("IN_PROGRESS session {} is expired. Auto-failing session.", session.getId());
                failSession(session.getId(), "Phiên giao hết hạn");
                log.info("No active session found for delivery man {} (expired session was auto-failed)", deliveryManId);
                return null;
            }
            log.info("Found IN_PROGRESS session {} for delivery man {}", session.getId(), deliveryManId);
            return toSessionResponse(session);
        }
        
        Optional<DeliverySession> createdSession = sessionRepository.findByDeliveryManIdAndStatus(deliveryManId, SessionStatus.CREATED);
        if (createdSession.isPresent()) {
            DeliverySession session = createdSession.get();
            // Check if session is expired and auto-fail it
            if (isSessionExpired(session)) {
                log.warn("CREATED session {} is expired. Auto-failing session.", session.getId());
                failSession(session.getId(), "Phiên giao hết hạn");
                log.info("No active session found for delivery man {} (expired session was auto-failed)", deliveryManId);
                return null;
            }
            log.info("Found CREATED session {} for delivery man {}", session.getId(), deliveryManId);
            return toSessionResponse(session);
        }
        
        log.info("No active session found for delivery man {}", deliveryManId);
        return null; // No active session
    }

    // --- HELPER METHODS ---

    /**
     * Kiểm tra xem session có hết hạn không.
     * Session hết hạn nếu:
     * 1. Đã qua 13 tiếng kể từ startTime, HOẶC
     * 2. Thời gian hiện tại đã vượt quá 21:00 cùng ngày với startTime
     */
    private boolean isSessionExpired(DeliverySession session) {
        if (session.getStartTime() == null) {
            return false; // Không có startTime thì không hết hạn
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = session.getStartTime();
        
        // Kiểm tra 1: Đã qua 13 tiếng
        LocalDateTime expirationTime = startTime.plusHours(SESSION_MAX_HOURS);
        if (now.isAfter(expirationTime)) {
            log.info("Session {} expired: {} hours passed (max: {} hours)", 
                session.getId(), java.time.Duration.between(startTime, now).toHours(), SESSION_MAX_HOURS);
            return true;
        }
        
        // Kiểm tra 2: Đã vượt quá 21:00 cùng ngày
        LocalDateTime sessionEndTime = startTime.toLocalDate().atTime(SESSION_END_TIME);
        if (now.isAfter(sessionEndTime)) {
            log.info("Session {} expired: current time {} is after session end time {}", 
                session.getId(), now, sessionEndTime);
            return true;
        }
        
        return false;
    }

    /**
     * Validate routing time khi số đơn > 5.
     * Không cho nhận đơn nếu:
     * 1. Tổng thời gian routing > 5 tiếng, HOẶC
     * 2. Tổng thời gian routing sẽ vượt quá thời gian phiên giao (21:00 cùng ngày)
     */
    private void validateRoutingTime(DeliverySession session, ParcelResponse newParcel) {
        try {
            // Lấy tất cả parcels trong session (bao gồm cả parcel mới)
            List<String> parcelIds = new ArrayList<>();
            for (DeliveryAssignment assignment : session.getAssignments()) {
                parcelIds.add(assignment.getParcelId());
            }
            parcelIds.add(newParcel.getId());

            // Lấy thông tin location của tất cả parcels
            List<RouteRequest.Waypoint> waypoints = new ArrayList<>();
            log.debug("Fetching location info for {} parcels for routing validation", parcelIds.size());
            long fetchStart = System.currentTimeMillis();
            for (String pid : parcelIds) {
                ParcelResponse parcel = parcelApiClient.fetchParcelResponse(pid);
                if (parcel == null || parcel.getLat() == null || parcel.getLon() == null) {
                    log.warn("Parcel {} does not have location information. Skipping routing validation.", pid);
                    continue;
                }
                
                RouteRequest.Waypoint waypoint = RouteRequest.Waypoint.builder()
                    .lat(parcel.getLat().doubleValue())
                    .lon(parcel.getLon().doubleValue())
                    .parcelId(pid)
                    .build();
                waypoints.add(waypoint);
            }
            log.debug("Fetched {} waypoints in {}ms", waypoints.size(), System.currentTimeMillis() - fetchStart);

            if (waypoints.size() < 2) {
                log.warn("Not enough waypoints for routing validation. Skipping.");
                return;
            }

            // Gọi zone-service để tính routing time
            RouteRequest routeRequest = RouteRequest.builder()
                .waypoints(waypoints)
                .steps(false)
                .annotations(false)
                .build();

            log.debug("Calling zone-service to calculate route for {} waypoints", waypoints.size());
            long routeStart = System.currentTimeMillis();
            RouteResponse routeResponse = zoneServiceClient.calculateRoute(routeRequest);
            log.debug("Route calculation completed in {}ms", System.currentTimeMillis() - routeStart);
            
            if (routeResponse == null || routeResponse.getRoute() == null) {
                log.warn("Failed to get routing response. Allowing parcel addition.");
                return;
            }

            // Lấy tổng thời gian routing (tính bằng giây)
            double totalDurationSeconds = 0;
            if (routeResponse.getRoute().getSummary() != null && 
                routeResponse.getRoute().getSummary().getTotalDuration() != null) {
                totalDurationSeconds = routeResponse.getRoute().getSummary().getTotalDuration();
            } else if (routeResponse.getRoute().getDuration() != null) {
                totalDurationSeconds = routeResponse.getRoute().getDuration();
            }

            // Chuyển đổi sang giờ
            double totalDurationHours = totalDurationSeconds / 3600.0;
            
            log.info("Session {} routing validation: total duration = {} hours ({} seconds)", 
                session.getId(), totalDurationHours, totalDurationSeconds);

            // Kiểm tra 1: Tổng thời gian > 5 tiếng
            if (totalDurationHours > MAX_DELIVERY_HOURS) {
                throw new IllegalStateException(
                    String.format("Không thể nhận thêm đơn: Tổng thời gian giao hàng (%.2f giờ) vượt quá giới hạn %d giờ.", 
                        totalDurationHours, MAX_DELIVERY_HOURS));
            }

            // Kiểm tra 2: Tổng thời gian sẽ vượt quá thời gian phiên giao (21:00 cùng ngày)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime estimatedEndTime = now.plusSeconds((long) totalDurationSeconds);
            LocalDateTime sessionEndTime = session.getStartTime().toLocalDate().atTime(SESSION_END_TIME);
            
            if (estimatedEndTime.isAfter(sessionEndTime)) {
                throw new IllegalStateException(
                    String.format("Không thể nhận thêm đơn: Thời gian giao hàng ước tính (%.2f giờ) sẽ vượt quá thời gian kết thúc phiên (21:00).", 
                        totalDurationHours));
            }

            log.info("Routing validation passed for session {}: {} hours, estimated end: {}", 
                session.getId(), totalDurationHours, estimatedEndTime);

        } catch (IllegalStateException e) {
            // Re-throw validation errors
            throw e;
        } catch (Exception e) {
            // Log error nhưng không block việc thêm đơn nếu routing service lỗi
            log.error("Failed to validate routing time for session {}: {}. Allowing parcel addition.", 
                session.getId(), e.getMessage());
            // Không throw exception để không block việc thêm đơn khi routing service có vấn đề
        }
    }

    // --- HELPER MAPPERS ---

    private SessionResponse toSessionResponse(DeliverySession session) {
        List<AssignmentResponse> assignmentResponses = session.getAssignments().stream()
            .map(this::toAssignmentResponse)
            .collect(Collectors.toList());
        
        long completedTasks = session.getAssignments().stream()
            .filter(a -> a.getStatus() == AssignmentStatus.COMPLETED).count();
            
        long failedTasks = session.getAssignments().stream()
            .filter(a -> a.getStatus() == AssignmentStatus.FAILED).count();

        // Enrich with delivery man info
        SessionResponse.DeliveryManInfo deliveryManInfo = null;
        try {
            DeliveryManResponse deliveryManResponse = userServiceClient.getDeliveryManByUserId(session.getDeliveryManId());
            if (deliveryManResponse != null) {
                String fullName = (deliveryManResponse.getFirstName() != null ? deliveryManResponse.getFirstName() : "")
                    + (deliveryManResponse.getLastName() != null ? " " + deliveryManResponse.getLastName() : "").trim();
                if (fullName.isEmpty()) {
                    fullName = deliveryManResponse.getUsername() != null ? deliveryManResponse.getUsername() : "Unknown";
                }
                
                deliveryManInfo = SessionResponse.DeliveryManInfo.builder()
                    .name(fullName)
                    .vehicleType(deliveryManResponse.getVehicleType())
                    .capacityKg(deliveryManResponse.getCapacityKg())
                    .phone(deliveryManResponse.getPhone())
                    .email(deliveryManResponse.getEmail())
                    .build();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch delivery man info for user {}: {}", session.getDeliveryManId(), e.getMessage());
            // Continue without delivery man info - don't fail the request
        }

        return SessionResponse.builder()
            .id(session.getId())
            .deliveryManId(session.getDeliveryManId())
            .status(session.getStatus())
            .startTime(session.getStartTime())
            .endTime(session.getEndTime())
            .totalTasks(session.getAssignments().size())
            .completedTasks((int) completedTasks)
            .failedTasks((int) failedTasks)
            .assignments(assignmentResponses)
            .deliveryMan(deliveryManInfo)
            .build();
    }

    private AssignmentResponse toAssignmentResponse(DeliveryAssignment assignment) {
        return AssignmentResponse.builder()
            .id(assignment.getId())
            .parcelId(assignment.getParcelId())
            .status(assignment.getStatus())
            .failReason(assignment.getFailReason())
            .scanedAt(assignment.getScanedAt())
            .updatedAt(assignment.getUpdatedAt())
            .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public PagedData<SessionResponse> searchSessionsV2(PagingRequestV2 request) {
        log.info("Searching delivery sessions with V2 filters: page={}, size={}", request.getPage(), request.getSize());
        
        // Build specification from V2 filters
        Specification<DeliverySession> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (request.getFiltersOrNull() != null) {
            Specification<DeliverySession> filterSpec = EnhancedQueryParserV2.parseFilterGroup(request.getFiltersOrNull(), DeliverySession.class);
            if (filterSpec != null) {
                spec = filterSpec;
            }
        }
        
        // Build sort
        Sort sort = buildSort(request.getSortsOrEmpty());
        
        // Build pageable
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        
        // Execute query
        Page<DeliverySession> page = sessionRepository.findAll(spec, pageable);
        
        // Convert to SessionResponse
        List<SessionResponse> sessionResponses = page.getContent().stream()
            .map(this::toSessionResponse)
            .collect(Collectors.toList());
        
        // Build paged data response
        PagedData.Paging<String> paging = PagedData.Paging.<String>builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .filters(request.getFiltersOrNull())
            .sorts(request.getSortsOrEmpty())
            .selected(request.getSelectedOrEmpty())
            .build();
        
        return PagedData.<SessionResponse>builder()
            .data(sessionResponses)
            .page(paging)
            .build();
    }
    
    /**
     * Build Sort from SortConfig list
     */
    private Sort buildSort(List<SortConfig> sortConfigs) {
        if (sortConfigs == null || sortConfigs.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "startTime"); // Default sort
        }
        
        List<Sort.Order> orders = sortConfigs.stream()
            .map(config -> {
                Sort.Direction direction = "desc".equalsIgnoreCase(config.getDirection()) 
                    ? Sort.Direction.DESC 
                    : Sort.Direction.ASC;
                return new Sort.Order(direction, config.getField());
            })
            .collect(Collectors.toList());
        
        return Sort.by(orders);
    }
}
