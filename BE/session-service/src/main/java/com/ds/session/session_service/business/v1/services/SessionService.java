package com.ds.session.session_service.business.v1.services;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final com.ds.session.session_service.infrastructure.kafka.EventProducer eventProducer;
    
    // Session expiration: 13 hours or before 21:00 on the same day
    private static final int SESSION_MAX_HOURS = 13;
    private static final LocalTime SESSION_END_TIME = LocalTime.of(21, 0);
    private static final int MAX_DELIVERY_HOURS = 5; // Maximum 5 hours for delivery routing 


    // =========================================================================
    // CẤU HÌNH CÔNG BẰNG & CHỐNG LỰA ĐƠN
    // =========================================================================
    
    // 1. Cấu hình Cân bằng tải (Fairness)
    private static final double FAIRNESS_THRESHOLD = 1.3; // Được phép nhận hơn mức trung bình 30%
    private static final int MIN_PARCELS_TO_APPLY_FAIRNESS = 5; // Chỉ áp dụng khi đã có ít nhất 5 đơn

    // 2. Cấu hình Chống lựa đơn (Anti-Cherry Picking)
    //Cần thay bằng tọa độ thực tế của Hub/Kho hiện tại
    private static final double WAREHOUSE_LAT = 10.7769; 
    private static final double WAREHOUSE_LON = 106.7009;
    
    private static final double EASY_ORDER_DISTANCE_KM = 3.0; // Đơn dưới 3km coi là đơn "ngon/dễ"
    private static final double MAX_EASY_RATIO = 0.6; // Không được quá 60% đơn là đơn dễ

    // =========================================================================

    @Override
    @Transactional
    public AssignmentResponse acceptParcelToSession(String deliveryManId, String parcelId) {
        long startTime = System.currentTimeMillis();
        log.debug("[session-service] [SessionService.acceptParcelToSession] Processing scan for parcel {} by shipper {}", parcelId, deliveryManId);

        // 1. Kiểm tra trạng thái đơn hàng từ ParcelService
        long fetchStart = System.currentTimeMillis();
        ParcelResponse parcelInfo = parcelApiClient.fetchParcelResponse(parcelId);
        log.debug("Fetched parcel info in {}ms", System.currentTimeMillis() - fetchStart);
        if (parcelInfo == null) {
            throw new IllegalStateException("Parcel " + parcelId + " does not exist.");
        }
        // Allow accepting parcels with status IN_WAREHOUSE or DELAYED
        String parcelStatus = parcelInfo.getStatus();
        if (!"IN_WAREHOUSE".equals(parcelStatus) && !"DELAYED".equals(parcelStatus)) {
            throw new IllegalStateException("Parcel " + parcelId + " is not IN_WAREHOUSE or DELAYED. Current status: " + parcelStatus);
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
                log.debug("[session-service] [SessionService.acceptParcelToSession] Parcel {} already in this active session ({}). Returning existing task.", 
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
            log.debug("[session-service] [SessionService.acceptParcelToSession] Found IN_PROGRESS session {} for shipper {}. Adding task.", inProgressSessionOpt.get().getId(), deliveryManId);
            sessionToUse = inProgressSessionOpt.get();
        } else if (createdSessionOpt.isPresent()) {
            // Nếu có phiên CREATED, sử dụng nó
            log.debug("[session-service] [SessionService.acceptParcelToSession] Found CREATED session {} for shipper {}. Adding task.", createdSessionOpt.get().getId(), deliveryManId);
            sessionToUse = createdSessionOpt.get();
        } else {
            // 4a. Logic: "scan đơn đầu tiên sẽ tạo phiên ở trạng thái CREATED"
            log.debug("[session-service] [SessionService.acceptParcelToSession] No active session found for shipper {}. Creating a new CREATED session.", deliveryManId);
            sessionToUse = DeliverySession.builder()
                .deliveryManId(deliveryManId)
                .status(SessionStatus.CREATED)
                .startTime(LocalDateTime.now())
                .build();
        }

        // 1. Kiểm tra Công bằng (Load Balancing)
        // Nếu shipper này đang ôm quá nhiều đơn so với anh em -> Chặn
        validateFairness(sessionToUse);

        // 2. Kiểm tra Chống lựa đơn (Anti-Cherry Picking)
        // Nếu shipper này chỉ toàn nhặt đơn gần -> Chặn
        validateCherryPicking(sessionToUse, parcelInfo); // parcelInfo lấy từ bước 1

        // 5. Tạo Assignment (Task) mới
        DeliveryAssignment newAssignment = DeliveryAssignment.builder()
            .parcelId(parcelId)
            .status(AssignmentStatus.IN_PROGRESS)
            .scanedAt(LocalDateTime.now())
            .build();
        
        // 6. Kiểm tra hết hạn session trước khi thêm đơn
        // Auto-fail disabled: removed automatic session expiration check
        // if (isSessionExpired(sessionToUse)) {
        //     log.debug("[session-service] [SessionService.acceptParcelToSession] Session {} is expired. Auto-failing session before adding parcel.", sessionToUse.getId());
        //     failSession(sessionToUse.getId(), "Phiên giao hết hạn");
        //     throw new IllegalStateException("Session has expired. Please create a new session.");
        // }

        // 7. Validation: Nếu số đơn > 5, kiểm tra routing time
        // int currentParcelCount = sessionToUse.getAssignments().size();
        // if (currentParcelCount >= 5) {
        //     log.debug("[session-service] [SessionService.acceptParcelToSession] Session {} has {} parcels. Validating routing time before adding new parcel.", 
        //         sessionToUse.getId(), currentParcelCount);
            
        //     // Validate routing time
        //     long routingStart = System.currentTimeMillis();
        //     validateRoutingTime(sessionToUse, parcelInfo);
        //     log.debug("[session-service] [SessionService.acceptParcelToSession] Routing validation completed in {}ms", System.currentTimeMillis() - routingStart);
        // }

        // 8. Liên kết task vào session
        sessionToUse.addAssignment(newAssignment);

        // 9. Gọi Parcel-Service để cập nhật trạng thái đơn hàng
        try {
            parcelEventPublisher.publish(parcelId, ParcelEvent.SCAN_QR);
        } catch (Exception e) {
            log.error("[session-service] [SessionService.acceptParcelToSession] Failed to publish parcel status event for parcel {}", parcelId, e);
            throw new RuntimeException("Failed to publish parcel status event: " + e.getMessage(), e);
        }

        // 10. Lưu session (và task mới sẽ được lưu theo nhờ CascadeType.ALL)
        sessionRepository.save(sessionToUse);
        long totalTime = System.currentTimeMillis() - startTime;
        log.debug("[session-service] [SessionService.acceptParcelToSession] Successfully added parcel {} to session {} in {}ms.", parcelId, sessionToUse.getId(), totalTime);

        // Trả về task vừa được tạo
        return toAssignmentResponse(newAssignment);
    }


    /**
     * GIẢI PHÁP 1: CÂN BẰNG TẢI (FAIRNESS)
     * Ngăn chặn việc 1 người nhận quá nhiều đơn trong khi người khác đói đơn.
     */
    private void validateFairness(DeliverySession currentSession) {
        int myLoad = currentSession.getAssignments().size();
        
        // Chưa đủ số lượng tối thiểu để đánh giá -> Bỏ qua
        if (myLoad < MIN_PARCELS_TO_APPLY_FAIRNESS) {
            return; 
        }

        // Lấy danh sách tất cả các session đang hoạt động (CREATED hoặc IN_PROGRESS)
        List<DeliverySession> activeSessions = sessionRepository.findByStatusIn(
            List.of(SessionStatus.CREATED, SessionStatus.IN_PROGRESS)
        );

        if (activeSessions.isEmpty()) return;

        // Tính tổng số đơn toàn hệ thống
        double totalParcels = activeSessions.stream()
            .mapToInt(s -> s.getAssignments().size())
            .sum();

        // Tính trung bình (Average Load)
        double averageLoad = totalParcels / activeSessions.size();
        
        // Tính giới hạn cho phép (Ví dụ: Trung bình 10 đơn * 1.3 = 13 đơn)
        double allowedLimit = Math.max(averageLoad * FAIRNESS_THRESHOLD, MIN_PARCELS_TO_APPLY_FAIRNESS);

        if (myLoad > allowedLimit) {
            throw new IllegalStateException(
                String.format("Bạn đang giữ %d đơn, vượt quá mức trung bình (%.1f). " +
                              "Vui lòng đợi đồng nghiệp nhận thêm đơn hoặc nhường đơn cho người mới.", 
                              myLoad, averageLoad));
        }
    }

    /**
     * GIẢI PHÁP 2: CHỐNG LỰA ĐƠN (ANTI-CHERRY PICKING)
     * Ngăn chặn việc chỉ chọn đơn Gần (Dễ) và chừa lại đơn Xa (Khó).
     */
    private void validateCherryPicking(DeliverySession session, ParcelResponse newParcel) {
        // Nếu đơn mới không có tọa độ -> Bỏ qua check (hoặc chặn tuỳ policy)
        if (newParcel.getLat() == null || newParcel.getLon() == null) return;

        // 1. Tính khoảng cách đơn mới đến kho
        double newDistance = calculateDistance(
            WAREHOUSE_LAT, WAREHOUSE_LON, 
            newParcel.getLat().doubleValue(), newParcel.getLon().doubleValue()
        ) / 1000.0; // Đổi ra km

        // Nếu đơn mới là đơn XA (Khó) -> Luôn cho phép nhận (Khuyến khích)
        if (newDistance >= EASY_ORDER_DISTANCE_KM) {
            return;
        }

        // 2. Nếu đơn mới là đơn GẦN (Dễ) -> Kiểm tra xem trong giỏ đã có bao nhiêu đơn gần rồi?
        List<DeliveryAssignment> assignments = session.getAssignments();
        int totalOrders = assignments.size();
        
        if (totalOrders < MIN_PARCELS_TO_APPLY_FAIRNESS) return; // Chưa đủ mẫu để chặn

        long easyOrderCount = 0;
        
        // *Lưu ý Performance*: Vòng lặp này gọi API ParcelService nhiều lần.
        // Tốt nhất: Nên lưu field 'distance' hoặc 'isEasy' vào bảng DeliveryAssignment lúc tạo.
        //  demo nen tính toán lại.
        for (DeliveryAssignment assignment : assignments) {
            try {
                ParcelResponse p = parcelApiClient.fetchParcelResponse(assignment.getParcelId());
                if (p != null && p.getLat() != null) {
                    double d = calculateDistance(WAREHOUSE_LAT, WAREHOUSE_LON, p.getLat().doubleValue(), p.getLon().doubleValue()) / 1000.0;
                    if (d < EASY_ORDER_DISTANCE_KM) {
                        easyOrderCount++;
                    }
                }
            } catch (Exception e) {
                // Skip lỗi lẻ tẻ
            }
        }

        // Tính tỷ lệ đơn dễ hiện tại
        double currentEasyRatio = (double) easyOrderCount / totalOrders;

        // Nếu tỷ lệ đơn dễ đã quá cao (ví dụ > 60%) -> Chặn không cho nhận thêm đơn dễ
        if (currentEasyRatio > MAX_EASY_RATIO) {
            throw new IllegalStateException(
                String.format("Tỷ lệ đơn gần của bạn đang là %.0f%% (Giới hạn %.0f%%). " +
                              "Vui lòng nhận đơn khác (>%skm) để cân bằng lộ trình.", 
                              currentEasyRatio * 100, MAX_EASY_RATIO * 100, EASY_ORDER_DISTANCE_KM));
        }
    }

    // --- Helper tính khoảng cách (Haversine) ---
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // Trả về mét
    }

    @Override
    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        log.debug("[session-service] [SessionService.createSession] Creating new session for delivery man: {}", request.getDeliveryManId());

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
        log.debug("[session-service] [SessionService.createSession] Session {} created with {} tasks.", savedSession.getId(), savedSession.getAssignments().size());

        return toSessionResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionResponse createSessionPrepared(String deliveryManId) {
        log.debug("[session-service] [SessionService.createSessionPrepared] Creating prepared session (CREATED status) for delivery man: {}", deliveryManId);

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
        log.debug("[session-service] [SessionService.createSessionPrepared] Prepared session {} created for delivery man {}.", savedSession.getId(), deliveryManId);

        return toSessionResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionResponse startSession(UUID sessionId) {
        log.debug("[session-service] [SessionService.startSession] Starting session {} (CREATED -> IN_PROGRESS)", sessionId);
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
        
        log.debug("[session-service] [SessionService.startSession] Session {} started. Updating {} parcels to ON_ROUTE status", sessionId, assignments.size());
        
        for (DeliveryAssignment assignment : assignments) {
            try {
                parcelEventPublisher.publish(assignment.getParcelId(), ParcelEvent.SCAN_QR);
                log.debug("Published SCAN_QR event for parcel {} to update to ON_ROUTE", assignment.getParcelId());
            } catch (Exception e) {
                log.error("Failed to publish SCAN_QR event for parcel {}: {}", assignment.getParcelId(), e.getMessage());
                // Don't throw - continue with other parcels
            }
        }
        
        log.debug("[session-service] [SessionService.startSession] Session {} started successfully. {} parcels updated to ON_ROUTE", sessionId, assignments.size());

        return toSessionResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionResponse completeSession(UUID sessionId) {
        log.debug("[session-service] [SessionService.completeSession] Completing session {}", sessionId);
        DeliverySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFound("Session not found: " + sessionId));

        // If session is already COMPLETED, return it directly (idempotent operation)
        if (session.getStatus() == SessionStatus.COMPLETED) {
            log.debug("[session-service] [SessionService.completeSession] Session {} is already COMPLETED. Returning existing session.", sessionId);
            return toSessionResponse(session);
        }
        
        // Only IN_PROGRESS or CREATED sessions can be completed
        if (session.getStatus() != SessionStatus.IN_PROGRESS && session.getStatus() != SessionStatus.CREATED) {
            log.debug("[session-service] [SessionService.completeSession] Session {} is not IN_PROGRESS or CREATED (current: {}). Cannot complete.", sessionId, session.getStatus());
            throw new IllegalStateException("Session " + sessionId + " must be IN_PROGRESS or CREATED to complete. Current status: " + session.getStatus());
        }
        
        // Kiểm tra lại (phòng hờ): Đảm bảo tất cả task đã xong (CREATED hoặc IN_PROGRESS)
        long pendingTasks = assignmentRepository.countPendingTasksBySessionId(session.getId());
        if (pendingTasks > 0) {
            throw new IllegalStateException("Cannot complete session " + sessionId + ": " + pendingTasks + " tasks are still pending.");
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setEndTime(LocalDateTime.now());
        DeliverySession savedSession = sessionRepository.save(session);
        
        // Handle DELAYED tasks: notify Parcel-Service to change DELAYED parcels back to IN_WAREHOUSE
        // This includes:
        // 1. Tasks that were DELAYED due to customer request (POSTPONE) during delivery
        // 2. Tasks that were DELAYED due to session failure (now handled by failSession)
        // Mâu thuẫn: tại sao session failure nằm trong complete và tại sao sinh ra DELAYED?
        List<DeliveryAssignment> delayedTasks = savedSession.getAssignments().stream()
            .filter(a -> a.getStatus().equals(AssignmentStatus.DELAYED))
            .toList();
        
        log.debug("[session-service] [SessionService.completeSession] Session {} completed. Processing {} DELAYED tasks to return to IN_WAREHOUSE", sessionId, delayedTasks.size());
        
        // for (DeliveryAssignment task : delayedTasks) {
        //     try {
        //         // END_SESSION event will change parcel from DELAYED to IN_WAREHOUSE
        //         parcelApiClient.changeParcelStatus(task.getParcelId(), ParcelEvent.END_SESSION);
        //         log.debug("[session-service] [SessionService.completeSession] Parcel {} returned to IN_WAREHOUSE from DELAYED", task.getParcelId());
        //     } catch (Exception e) {
        //         log.error("Failed to call Parcel-Service for parcel {}: {}", task.getParcelId(), e.getMessage());
        //     }
        // }
        
        // Publish SessionCompletedEvent to notify all related clients/shippers
        publishSessionCompletedEvent(savedSession);
        
        return toSessionResponse(savedSession);
    }

    // @Override
    // @Transactional
    // public SessionResponse failSession(UUID sessionId, String reason) {
    //     log.debug("[session-service] [SessionService.failSession] Failing session {} due to: {}", sessionId, reason);
    //     DeliverySession session = sessionRepository.findById(sessionId)
    //         .orElseThrow(() -> new ResourceNotFound("Session not found: " + sessionId));
        
    //     // Allow failing CREATED or IN_PROGRESS sessions
    //     // Session fail is when: expired, shipper cancels, or admin cancels
    //     // NOT when individual parcels fail
    //     if (session.getStatus() != SessionStatus.IN_PROGRESS && session.getStatus() != SessionStatus.CREATED) {
    //         log.debug("[session-service] [SessionService.failSession] Session {} is already in state {}. Cannot fail.", sessionId, session.getStatus());
    //         throw new IllegalStateException("Session " + sessionId + " must be in IN_PROGRESS or CREATED status to fail. Current status: " + session.getStatus());
    //     }
        
    //     session.setStatus(SessionStatus.FAILED);
    //     session.setEndTime(LocalDateTime.now());
        
    //     DeliverySession savedSession = sessionRepository.save(session);

    //     // When session fails (expired, cancelled by shipper/admin), all undelivered parcels (IN_PROGRESS) 
    //     // must be set to DELAYED status, not FAILED
    //     List<DeliveryAssignment> pendingTasks = assignmentRepository.findBySession_IdAndStatus(sessionId, AssignmentStatus.IN_PROGRESS);
    //     log.debug("[session-service] [SessionService.failSession] Session {} failed. Setting {} pending tasks to DELAYED status", sessionId, pendingTasks.size());
        
    //     for (DeliveryAssignment task : pendingTasks) {
    //         // Set assignment status to DELAYED (not FAILED)
    //         task.setStatus(AssignmentStatus.DELAYED);
    //         task.setFailReason("Session Failed: " + reason);
    //         assignmentRepository.save(task);

    //         // Notify Parcel-Service: parcel should be delayed (POSTPONE event)
    //         // This will change parcel status from ON_ROUTE to DELAYED
    //         try {
    //             parcelApiClient.changeParcelStatus(task.getParcelId(), ParcelEvent.POSTPONE);
    //             log.debug("[session-service] [SessionService.failSession] Parcel {} set to DELAYED due to session failure", task.getParcelId());
    //         } catch (Exception e) {
    //             log.error("[session-service] [SessionService.failSession] Failed to call Parcel-Service for parcel {}", task.getParcelId(), e);
    //         }
    //     }
        
    //     return toSessionResponse(savedSession);
    // }

    @Override
    @Transactional
    public SessionResponse failSession(UUID sessionId, String reason) {

        DeliverySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFound("Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.IN_PROGRESS
            && session.getStatus() != SessionStatus.CREATED) {
            throw new IllegalStateException(
                "Session must be IN_PROGRESS or CREATED to fail. Current: "
                    + session.getStatus());
        }

        // 🔎 Phase decision dựa vào dữ liệu hiện tại
        List<DeliveryAssignment> inProgressTasks =
            assignmentRepository.findBySession_IdAndStatus(
                sessionId, AssignmentStatus.IN_PROGRESS);

        if (!inProgressTasks.isEmpty()) {
            // =====================
            // PHASE 1: soft-fail
            // =====================
            log.info(
                "FailSession phase 1: converting {} IN_PROGRESS assignments to FAILED",
                inProgressTasks.size());

            for (DeliveryAssignment task : inProgressTasks) {
                task.setStatus(AssignmentStatus.FAILED);
                task.setFailReason("Session failed: " + reason);
                assignmentRepository.save(task);

                try {
                    parcelApiClient.changeParcelStatus(
                        task.getParcelId(), ParcelEvent.CAN_NOT_DELIVERY);
                } catch (Exception e) {
                    log.error(
                        "Failed to notify parcel-service for parcel {}",
                        task.getParcelId(), e);
                }
            }

            // ❗ KHÔNG đóng session ở phase này
            return toSessionResponse(session);
        }

        // =====================
        // PHASE 2: hard-close
        // =====================
        if (session.getStatus() == SessionStatus.FAILED) {
            // idempotent safety
            return toSessionResponse(session);
        }

        log.info("FailSession phase 2: closing session {}", sessionId);

        session.setStatus(SessionStatus.FAILED);
        session.setEndTime(LocalDateTime.now());

        DeliverySession saved = sessionRepository.save(session);
        return toSessionResponse(saved);
    }

    
    /**
     * Publish SessionCompletedEvent to communication-service
     * This event triggers notifications to all related clients/shippers
     */
    private void publishSessionCompletedEvent(DeliverySession session) {
        try {
            // Collect parcel IDs and receiver IDs from assignments
            List<String> parcelIds = new ArrayList<>();
            List<String> receiverIds = new ArrayList<>();
            
            for (DeliveryAssignment assignment : session.getAssignments()) {
                parcelIds.add(assignment.getParcelId());
                // Fetch parcel to get receiverId
                try {
                    ParcelResponse parcel = parcelApiClient.fetchParcelResponse(assignment.getParcelId());
                    if (parcel != null && parcel.getReceiverId() != null && !receiverIds.contains(parcel.getReceiverId())) {
                        receiverIds.add(parcel.getReceiverId());
                    }
                } catch (Exception e) {
                    log.debug("[session-service] [SessionService.completeSession] Failed to fetch parcel {} for receiverId: {}", assignment.getParcelId(), e.getMessage());
                }
            }
            
            long completedTasks = session.getAssignments().stream()
                .filter(a -> a.getStatus() == AssignmentStatus.COMPLETED).count();
            long failedTasks = session.getAssignments().stream()
                .filter(a -> a.getStatus() == AssignmentStatus.FAILED).count();
            long delayedTasks = session.getAssignments().stream()
                .filter(a -> a.getStatus() == AssignmentStatus.DELAYED).count();
            
            com.ds.session.session_service.common.entities.dto.event.SessionCompletedEvent completedEvent = 
                com.ds.session.session_service.common.entities.dto.event.SessionCompletedEvent.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .sessionId(session.getId().toString())
                    .deliveryManId(session.getDeliveryManId())
                    .completedAt(session.getEndTime() != null ? session.getEndTime() : LocalDateTime.now())
                    .startTime(session.getStartTime())
                    .endTime(session.getEndTime())
                    .totalTasks(session.getAssignments().size())
                    .completedTasks((int) completedTasks)
                    .failedTasks((int) failedTasks)
                    .delayedTasks((int) delayedTasks)
                    .parcelIds(parcelIds)
                    .receiverIds(receiverIds)
                    .createdAt(java.time.Instant.now())
                    .sourceService("session-service")
                    .build();
            
            eventProducer.publishSessionCompleted(session.getId().toString(), completedEvent);
            log.debug("[session-service] [SessionService.completeSession] Published SessionCompletedEvent for session: {}", session.getId());
        } catch (Exception e) {
            log.error("[session-service] [SessionService.completeSession] Failed to publish SessionCompletedEvent for session {}", 
                session.getId(), e);
        }
    }


    @Override
    public SessionResponse getSessionById(UUID sessionId) {
        DeliverySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));
        
        // Auto-fail disabled: removed automatic session expiration check
        // Check if session is expired and auto-fail it
        // if (isSessionExpired(session)) {
        //     log.debug("[session-service] [SessionService.getSessionById] Session {} is expired. Auto-failing session.", sessionId);
        //     failSession(sessionId, "Phiên giao hết hạn");
        //     // Re-fetch the session after failing
        //     session = sessionRepository.findById(sessionId)
        //         .orElseThrow(() -> new EntityNotFoundException("Session not found: " + sessionId));
        // }
        
        return toSessionResponse(session);
    }
    
    @Override
    public SessionResponse getActiveSession(String deliveryManId) {
        log.debug("[session-service] [SessionService.getActiveSession] Getting active session for delivery man: {}", deliveryManId);
        
        // Try IN_PROGRESS first, then CREATED
        Optional<DeliverySession> inProgressSession = sessionRepository.findByDeliveryManIdAndStatus(deliveryManId, SessionStatus.IN_PROGRESS);
        if (inProgressSession.isPresent()) {
            DeliverySession session = inProgressSession.get();
            // Auto-fail disabled: removed automatic session expiration check
            // Check if session is expired and auto-fail it
            // if (isSessionExpired(session)) {
            //     log.debug("[session-service] [SessionService.getActiveSession] IN_PROGRESS session {} is expired. Auto-failing session.", session.getId());
            //     failSession(session.getId(), "Phiên giao hết hạn");
            //     log.debug("[session-service] [SessionService.getActiveSession] No active session found for delivery man {} (expired session was auto-failed)", deliveryManId);
            //     return null;
            // }
            log.debug("[session-service] [SessionService.getActiveSession] Found IN_PROGRESS session {} for delivery man {}", session.getId(), deliveryManId);
            return toSessionResponse(session);
        }
        
        Optional<DeliverySession> createdSession = sessionRepository.findByDeliveryManIdAndStatus(deliveryManId, SessionStatus.CREATED);
        if (createdSession.isPresent()) {
            DeliverySession session = createdSession.get();
            // Auto-fail disabled: removed automatic session expiration check
            // Check if session is expired and auto-fail it
            // if (isSessionExpired(session)) {
            //     log.debug("[session-service] [SessionService.getActiveSession] CREATED session {} is expired. Auto-failing session.", session.getId());
            //     failSession(session.getId(), "Phiên giao hết hạn");
            //     log.debug("[session-service] [SessionService.getActiveSession] No active session found for delivery man {} (expired session was auto-failed)", deliveryManId);
            //     return null;
            // }
            log.debug("[session-service] [SessionService.getActiveSession] Found CREATED session {} for delivery man {}", session.getId(), deliveryManId);
            return toSessionResponse(session);
        }
        
        log.debug("[session-service] [SessionService.getActiveSession] No active session found for delivery man {}", deliveryManId);
        return null; // No active session
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<SessionResponse> getAllSessionsForDeliveryMan(String deliveryManId, String excludeParcelId) {
        log.debug("[session-service] [SessionService.getAllSessionsForDeliveryMan] Getting all sessions for delivery man {} (excludeParcelId: {})", deliveryManId, excludeParcelId);
        
        // Use Specification to query all sessions by deliveryManId
        Specification<DeliverySession> spec = (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("deliveryManId"), deliveryManId);
        
        // Query all sessions for this delivery man
        List<DeliverySession> allSessions = sessionRepository.findAll(spec);
        log.debug("[session-service] [SessionService.getAllSessionsForDeliveryMan] Found {} sessions for delivery man {}", allSessions.size(), deliveryManId);
        
        // Filter out sessions containing excludeParcelId if provided
        List<DeliverySession> filteredSessions = allSessions;
        if (excludeParcelId != null && !excludeParcelId.isEmpty()) {
            filteredSessions = allSessions.stream()
                .filter(session -> {
                    // Check if session contains the parcel to exclude
                    boolean containsParcel = session.getAssignments().stream()
                        .anyMatch(assignment -> excludeParcelId.equals(assignment.getParcelId()));
                    return !containsParcel;
                })
                .collect(Collectors.toList());
            log.debug("[session-service] [SessionService.getAllSessionsForDeliveryMan] After excluding parcel {}, {} sessions remain", excludeParcelId, filteredSessions.size());
        }
        
        // Convert to SessionResponse
        List<SessionResponse> sessionResponses = filteredSessions.stream()
            .map(this::toSessionResponse)
            .sorted((a, b) -> {
                // Sort by startTime descending (newest first)
                LocalDateTime aStart = a.getStartTime() != null ? a.getStartTime() : LocalDateTime.MIN;
                LocalDateTime bStart = b.getStartTime() != null ? b.getStartTime() : LocalDateTime.MIN;
                return bStart.compareTo(aStart);
            })
            .collect(Collectors.toList());
        
        log.debug("[session-service] [SessionService.getAllSessionsForDeliveryMan] Returning {} sessions for delivery man {}", sessionResponses.size(), deliveryManId);
        return sessionResponses;
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
            log.debug("[session-service] [SessionService.isSessionExpired] Session {} expired: {} hours passed (max: {} hours)", 
                session.getId(), java.time.Duration.between(startTime, now).toHours(), SESSION_MAX_HOURS);
            return true;
        }
        
        // Kiểm tra 2: Đã vượt quá 21:00 cùng ngày
        LocalDateTime sessionEndTime = startTime.toLocalDate().atTime(SESSION_END_TIME);
        if (now.isAfter(sessionEndTime)) {
            log.debug("[session-service] [SessionService.isSessionExpired] Session {} expired: current time {} is after session end time {}", 
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
            // Check for assignments with MOVE_TO_END flag
            List<RouteRequest.Waypoint> waypoints = new ArrayList<>();
            List<RouteRequest.Waypoint> moveToEndWaypoints = new ArrayList<>(); // Parcels to move to end
            log.debug("Fetching location info for {} parcels for routing validation", parcelIds.size());
            long fetchStart = System.currentTimeMillis();
            for (String pid : parcelIds) {
                ParcelResponse parcel = parcelApiClient.fetchParcelResponse(pid);
                if (parcel == null || parcel.getLat() == null || parcel.getLon() == null) {
                    log.debug("[session-service] [SessionService.validateRoutingForSession] Parcel {} does not have location information. Skipping routing validation.", pid);
                    continue;
                }
                
                // Check if this assignment has MOVE_TO_END flag
                boolean moveToEnd = false;
                try {
                    DeliveryAssignment assignment = assignmentRepository.findBySession_IdAndParcelId(session.getId(), pid).orElse(null);
                    if (assignment != null && assignment.getFailReason() != null && assignment.getFailReason().contains("[MOVE_TO_END]")) {
                        moveToEnd = true;
                        log.debug("Parcel {} has MOVE_TO_END flag, will be moved to end of route", pid);
                    }
                } catch (Exception e) {
                    log.debug("Could not check MOVE_TO_END flag for parcel {}: {}", pid, e.getMessage());
                }
                
                RouteRequest.Waypoint waypoint = RouteRequest.Waypoint.builder()
                    .lat(parcel.getLat().doubleValue())
                    .lon(parcel.getLon().doubleValue())
                    .parcelId(pid)
                    .moveToEnd(moveToEnd) // Set flag for zone service
                    .build();
                
                if (moveToEnd) {
                    moveToEndWaypoints.add(waypoint); // Add to end list
                } else {
                    waypoints.add(waypoint); // Add to normal list
                }
            }
            // Add moveToEnd waypoints at the end
            waypoints.addAll(moveToEndWaypoints);
            log.debug("Fetched {} waypoints in {}ms", waypoints.size(), System.currentTimeMillis() - fetchStart);

            if (waypoints.size() < 2) {
                log.debug("[session-service] [SessionService.validateRoutingForSession] Not enough waypoints for routing validation. Skipping.");
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
                log.debug("[session-service] [SessionService.validateRoutingForSession] Failed to get routing response. Allowing parcel addition.");
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
            
            log.debug("[session-service] [SessionService.validateRoutingForSession] Session {} routing validation: total duration = {} hours ({} seconds)", 
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

            log.debug("[session-service] [SessionService.validateRoutingForSession] Routing validation passed for session {}: {} hours, estimated end: {}", 
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
            log.debug("[session-service] [SessionService.toSessionResponse] Failed to fetch delivery man info for user {}: {}", session.getDeliveryManId(), e.getMessage());
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
        log.debug("[session-service] [SessionService.searchDeliverySessionsV2] Searching delivery sessions with V2 filters: page={}, size={}", request.getPage(), request.getSize());
        
        // Build specification from V2 filters
        Specification<DeliverySession> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (request.getFiltersOrNull() != null) {
            log.debug("[session-service] [SessionService.searchDeliverySessionsV2] Parsing filters: {}", request.getFiltersOrNull());
            Specification<DeliverySession> filterSpec = EnhancedQueryParserV2.parseFilterGroup(request.getFiltersOrNull(), DeliverySession.class);
            if (filterSpec != null) {
                spec = filterSpec;
                log.debug("[session-service] [SessionService.searchDeliverySessionsV2] Filter specification created successfully");
            } else {
                log.debug("[session-service] [SessionService.searchDeliverySessionsV2] Filter specification is null, using default (no filter)");
            }
        } else {
            log.debug("[session-service] [SessionService.searchDeliverySessionsV2] No filters provided");
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
    
    @Override
    @Transactional(readOnly = true)
    public com.ds.session.session_service.common.entities.dto.response.DeliveryTimeResponse calculateDeliveryTime(
            com.ds.session.session_service.common.entities.dto.request.CalculateDeliveryTimeRequest request) {
        log.debug("[session-service] [SessionService.calculateDeliveryTime] Calculating delivery time for {} parcels", request.getParcelIds().size());
        
        try {
            // 1. Fetch parcel information
            // Check for assignments with MOVE_TO_END flag
            List<RouteRequest.Waypoint> waypoints = new ArrayList<>();
            List<RouteRequest.Waypoint> moveToEndWaypoints = new ArrayList<>(); // Parcels to move to end
            for (String parcelId : request.getParcelIds()) {
                ParcelResponse parcel = parcelApiClient.fetchParcelResponse(parcelId);
                if (parcel == null || parcel.getLat() == null || parcel.getLon() == null) {
                    log.debug("[session-service] [SessionService.calculateDeliveryTime] Parcel {} does not have location information. Skipping.", parcelId);
                    continue;
                }
                
                // Check if this assignment has MOVE_TO_END flag (if in a session)
                // Note: For calculateDeliveryTime API, we don't have session context, so moveToEnd will be null
                boolean moveToEnd = false; // Default to false for calculateDeliveryTime API
                
                RouteRequest.Waypoint waypoint = RouteRequest.Waypoint.builder()
                    .lat(parcel.getLat().doubleValue())
                    .lon(parcel.getLon().doubleValue())
                    .parcelId(parcelId)
                    .moveToEnd(moveToEnd ? true : null) // Set flag for zone service (nullable)
                    .build();
                
                if (moveToEnd) {
                    moveToEndWaypoints.add(waypoint); // Add to end list
                } else {
                    waypoints.add(waypoint); // Add to normal list
                }
            }
            // Add moveToEnd waypoints at the end
            waypoints.addAll(moveToEndWaypoints);
            
            if (waypoints.isEmpty()) {
                throw new IllegalStateException("No valid parcels with location information found");
            }
            
            // 2. Add current location as starting point if provided
            if (request.getCurrentLat() != null && request.getCurrentLon() != null) {
                RouteRequest.Waypoint startPoint = RouteRequest.Waypoint.builder()
                    .lat(request.getCurrentLat())
                    .lon(request.getCurrentLon())
                    .parcelId("START")
                    .build();
                waypoints.add(0, startPoint); // Add at the beginning
            }
            
            if (waypoints.size() < 2) {
                // If only one waypoint, return minimal response
                long bufferSeconds = 5L * 60 * request.getParcelIds().size(); // 5 minutes per parcel
                LocalDateTime now = LocalDateTime.now();
                return com.ds.session.session_service.common.entities.dto.response.DeliveryTimeResponse.builder()
                    .totalDurationSeconds(bufferSeconds)
                    .totalDurationMinutes(bufferSeconds / 60)
                    .estimatedCompletionTime(now.plusSeconds(bufferSeconds))
                    .routeDurationSeconds(0L)
                    .bufferSeconds(bufferSeconds)
                    .parcelCount(request.getParcelIds().size())
                    .build();
            }
            
            // 3. Calculate route from zone service
            RouteRequest routeRequest = RouteRequest.builder()
                .waypoints(waypoints)
                .steps(false)
                .annotations(false)
                .build();
            
            RouteResponse routeResponse = zoneServiceClient.calculateRoute(routeRequest);
            
            if (routeResponse == null || routeResponse.getRoute() == null) {
                throw new IllegalStateException("Failed to calculate route from zone service");
            }
            
            // 4. Get route duration
            double routeDurationSeconds = 0;
            if (routeResponse.getRoute().getSummary() != null && 
                routeResponse.getRoute().getSummary().getTotalDuration() != null) {
                routeDurationSeconds = routeResponse.getRoute().getSummary().getTotalDuration();
            } else if (routeResponse.getRoute().getDuration() != null) {
                routeDurationSeconds = routeResponse.getRoute().getDuration();
            }
            
            // 5. Calculate buffer: 5 minutes * number of remaining parcels
            long bufferSeconds = 5L * 60 * request.getParcelIds().size(); // 5 minutes per parcel
            
            // 6. Total duration = route duration + buffer
            long totalDurationSeconds = (long) routeDurationSeconds + bufferSeconds;
            
            // 7. Estimated completion time = current time + total duration
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime estimatedCompletionTime = now.plusSeconds(totalDurationSeconds);
            
            log.debug("[session-service] [SessionService.calculateDeliveryTime] Delivery time calculation: route={}s, buffer={}s, total={}s, estimated={}", 
                (long) routeDurationSeconds, bufferSeconds, totalDurationSeconds, estimatedCompletionTime);
            
            return com.ds.session.session_service.common.entities.dto.response.DeliveryTimeResponse.builder()
                .totalDurationSeconds(totalDurationSeconds)
                .totalDurationMinutes(totalDurationSeconds / 60)
                .estimatedCompletionTime(estimatedCompletionTime)
                .routeDurationSeconds((long) routeDurationSeconds)
                .bufferSeconds(bufferSeconds)
                .parcelCount(request.getParcelIds().size())
                .build();
                
        } catch (Exception e) {
            log.error("Failed to calculate delivery time: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate delivery time: " + e.getMessage(), e);
        }
    }
    
    /**
     * Transfer a parcel from current shipper to another shipper
     * Only allows transferring ON_ROUTE parcels
     */
    @Override
    @Transactional
    public AssignmentResponse transferParcel(String fromDeliveryManId, 
            com.ds.session.session_service.common.entities.dto.request.TransferParcelRequest request) {
        log.debug("[session-service] [SessionService.transferParcelToSession] Shipper {} transferring parcel {} to session {}", fromDeliveryManId, request.getParcelId(), request.getTargetSessionId());
        
        // 1. Verify parcel exists and is ON_ROUTE
        ParcelResponse parcelInfo = parcelApiClient.fetchParcelResponse(request.getParcelId());
        if (parcelInfo == null) {
            throw new IllegalStateException("Parcel " + request.getParcelId() + " does not exist.");
        }
        if (!"ON_ROUTE".equals(parcelInfo.getStatus())) {
            throw new IllegalStateException("Parcel " + request.getParcelId() + " must be ON_ROUTE to transfer. Current status: " + parcelInfo.getStatus());
        }
        
        // 2. Find IN_PROGRESS assignment of fromDeliveryManId for this parcel
        DeliveryAssignment sourceAssignment = assignmentRepository
            .findInProgressAssignmentByParcelIdAndDeliveryManId(request.getParcelId(), fromDeliveryManId)
            .orElseThrow(() -> new ResourceNotFound("IN_PROGRESS assignment not found for parcel " + request.getParcelId() + " and shipper " + fromDeliveryManId));
        
        // 3. Verify assignment belongs to fromDeliveryManId
        if (!sourceAssignment.getSession().getDeliveryManId().equals(fromDeliveryManId)) {
            throw new IllegalStateException("Assignment does not belong to shipper " + fromDeliveryManId);
        }
        
        // 4. Find target session
        UUID targetSessionId = UUID.fromString(request.getTargetSessionId());
        DeliverySession targetSession = sessionRepository.findById(targetSessionId)
            .orElseThrow(() -> new ResourceNotFound("Target session not found: " + request.getTargetSessionId()));
        
        // 5. Verify target session is active and belongs to different shipper
        if (targetSession.getStatus() != SessionStatus.CREATED && targetSession.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Target session must be CREATED or IN_PROGRESS. Current status: " + targetSession.getStatus());
        }
        if (targetSession.getDeliveryManId().equals(fromDeliveryManId)) {
            throw new IllegalStateException("Cannot transfer parcel to your own session");
        }
        
        // 6. Mark source assignment as FAILED
        sourceAssignment.setStatus(AssignmentStatus.FAILED);
        sourceAssignment.setFailReason("TRANSFERRED to shipper " + targetSession.getDeliveryManId());
        assignmentRepository.save(sourceAssignment);
        log.info("Marked source assignment {} as FAILED (TRANSFERRED)", sourceAssignment.getId());
        
        // 7. Create new assignment for target session
        DeliveryAssignment newAssignment = DeliveryAssignment.builder()
            .parcelId(request.getParcelId())
            .status(AssignmentStatus.IN_PROGRESS)
            .scanedAt(LocalDateTime.now())
            .build();
        
        targetSession.addAssignment(newAssignment);
        sessionRepository.save(targetSession);
        assignmentRepository.save(newAssignment);
        log.debug("[session-service] [SessionService.transferParcelToSession] Created new assignment {} for target session {}", newAssignment.getId(), targetSessionId);
        
        // 8. Publish parcel event (keep ON_ROUTE status)
        try {
            parcelEventPublisher.publish(request.getParcelId(), ParcelEvent.SCAN_QR);
        } catch (Exception e) {
            log.error("Failed to publish parcel event for transferred parcel {}: {}", request.getParcelId(), e.getMessage());
        }
        
        return toAssignmentResponse(newAssignment);
    }
    
    /**
     * Accept a transferred parcel by scanning source session QR
     */
    @Override
    @Transactional
    public AssignmentResponse acceptTransferredParcel(String toDeliveryManId,
            com.ds.session.session_service.common.entities.dto.request.AcceptTransferredParcelRequest request) {
        log.debug("[session-service] [SessionService.acceptTransferredParcel] Shipper {} accepting transferred parcel {} from session {}", toDeliveryManId, request.getParcelId(), request.getSourceSessionId());
        
        // 1. Verify parcel exists and is ON_ROUTE
        ParcelResponse parcelInfo = parcelApiClient.fetchParcelResponse(request.getParcelId());
        if (parcelInfo == null) {
            throw new IllegalStateException("Parcel " + request.getParcelId() + " does not exist.");
        }
        if (!"ON_ROUTE".equals(parcelInfo.getStatus())) {
            throw new IllegalStateException("Parcel " + request.getParcelId() + " must be ON_ROUTE to accept transfer. Current status: " + parcelInfo.getStatus());
        }
        
        // 2. Find source session
        UUID sourceSessionId = UUID.fromString(request.getSourceSessionId());
        DeliverySession sourceSession = sessionRepository.findById(sourceSessionId)
            .orElseThrow(() -> new ResourceNotFound("Source session not found: " + request.getSourceSessionId()));
        
        // 3. Find IN_PROGRESS assignment in source session
        DeliveryAssignment sourceAssignment = assignmentRepository
            .findInProgressAssignmentBySessionIdAndParcelId(sourceSessionId, request.getParcelId())
            .orElseThrow(() -> new ResourceNotFound("IN_PROGRESS assignment not found for parcel " + request.getParcelId() + " in session " + request.getSourceSessionId()));
        
        // 4. Verify source assignment is IN_PROGRESS
        if (sourceAssignment.getStatus() != AssignmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Source assignment is not IN_PROGRESS. Current status: " + sourceAssignment.getStatus());
        }
        
        // 5. Mark source assignment as FAILED
        sourceAssignment.setStatus(AssignmentStatus.FAILED);
        sourceAssignment.setFailReason("TRANSFERRED to shipper " + toDeliveryManId);
        assignmentRepository.save(sourceAssignment);
        log.info("Marked source assignment {} as FAILED (TRANSFERRED)", sourceAssignment.getId());
        
        // 6. Find or create session for toDeliveryManId
        Optional<DeliverySession> inProgressSessionOpt = sessionRepository.findByDeliveryManIdAndStatus(toDeliveryManId, SessionStatus.IN_PROGRESS);
        Optional<DeliverySession> createdSessionOpt = sessionRepository.findByDeliveryManIdAndStatus(toDeliveryManId, SessionStatus.CREATED);
        
        DeliverySession targetSession;
        if (inProgressSessionOpt.isPresent()) {
            targetSession = inProgressSessionOpt.get();
            log.debug("[session-service] [SessionService.acceptTransferredParcel] Using existing IN_PROGRESS session {} for shipper {}", targetSession.getId(), toDeliveryManId);
        } else if (createdSessionOpt.isPresent()) {
            targetSession = createdSessionOpt.get();
            log.debug("[session-service] [SessionService.acceptTransferredParcel] Using existing CREATED session {} for shipper {}", targetSession.getId(), toDeliveryManId);
        } else {
            // Create new session
            targetSession = DeliverySession.builder()
                .deliveryManId(toDeliveryManId)
                .status(SessionStatus.CREATED)
                .startTime(LocalDateTime.now())
                .build();
            sessionRepository.save(targetSession);
            log.debug("[session-service] [SessionService.acceptTransferredParcel] Created new CREATED session {} for shipper {}", targetSession.getId(), toDeliveryManId);
        }
        
        // 7. Create new assignment for target session
        DeliveryAssignment newAssignment = DeliveryAssignment.builder()
            .parcelId(request.getParcelId())
            .status(AssignmentStatus.IN_PROGRESS)
            .scanedAt(LocalDateTime.now())
            .build();
        
        targetSession.addAssignment(newAssignment);
        sessionRepository.save(targetSession);
        assignmentRepository.save(newAssignment);
        log.debug("[session-service] [SessionService.acceptTransferredParcel] Created new assignment {} for target session {}", newAssignment.getId(), targetSession.getId());
        
        // 8. Publish parcel event (keep ON_ROUTE status)
        try {
            parcelEventPublisher.publish(request.getParcelId(), ParcelEvent.SCAN_QR);
        } catch (Exception e) {
            log.error("Failed to publish parcel event for transferred parcel {}: {}", request.getParcelId(), e.getMessage());
        }
        
        return toAssignmentResponse(newAssignment);
    }


    @Override
    public List<AssignmentResponse> listAssignmentsMustReturnWarehouse(String sessionId) {
        DeliverySession session = sessionRepository.findById(UUID.fromString(sessionId))
            .orElseThrow(() -> new ResourceNotFound("Session not found: " + sessionId));
        return assignmentRepository.findBySession_IdAndStatusIn(
            UUID.fromString(sessionId), 
            List.of (AssignmentStatus.FAILED, AssignmentStatus.DELAYED)).stream()
            .map(this::toAssignmentResponse)
            .collect(Collectors.toList());
    }
}
