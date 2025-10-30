package com.ds.session.session_service.business.v1.services;

import java.time.LocalDateTime;
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
import com.ds.session.session_service.common.entities.dto.request.CreateSessionRequest;
import com.ds.session.session_service.common.entities.dto.response.AssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.SessionResponse;
import com.ds.session.session_service.common.enums.AssignmentStatus;
import com.ds.session.session_service.common.enums.ParcelEvent;
import com.ds.session.session_service.common.enums.SessionStatus;
import com.ds.session.session_service.common.exceptions.ResourceNotFound;
import com.ds.session.session_service.common.interfaces.ISessionService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService implements ISessionService {

    private final DeliverySessionRepository sessionRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final ParcelServiceClient parcelApiClient; 


    @Override
    @Transactional
    public AssignmentResponse acceptParcelToSession(String deliveryManId, String parcelId) {
        log.info("Processing scan for parcel {} by shipper {}", parcelId, deliveryManId);

        // 1. Kiểm tra trạng thái đơn hàng từ ParcelService
        ParcelResponse parcelInfo = parcelApiClient.fetchParcelResponse(parcelId);
        if (parcelInfo == null || !"IN_WAREHOUSE".equals(parcelInfo.getStatus())) {
            throw new IllegalStateException("Parcel " + parcelId + " is not IN_WAREHOUSE or does not exist. Current status: " + (parcelInfo != null ? parcelInfo.getStatus() : "N/A"));
        }

        // 2. Kiểm tra xem đơn hàng này đã thuộc một phiên (session) ĐANG HOẠT ĐỘNG
        // của BẤT KỲ shipper nào khác chưa.
        Optional<DeliveryAssignment> existingActiveAssignment = assignmentRepository.findActiveByParcelId(parcelId, SessionStatus.IN_PROGRESS);
        if (existingActiveAssignment.isPresent()) {
            // Nếu đơn hàng đã được gán cho chính shipper này (quét lại)
            if (existingActiveAssignment.get().getSession().getDeliveryManId().equals(deliveryManId)) {
                log.warn("Parcel {} already in this active session. Returning existing task.", parcelId);
                return toAssignmentResponse(existingActiveAssignment.get());
            }
            // Nếu đơn hàng bị gán cho shipper khác
            throw new IllegalStateException("Parcel " + parcelId + " is already in an active session of another delivery man.");
        }
        
        // 3. Tìm phiên (session) đang IN_PROGRESS của shipper này
        Optional<DeliverySession> activeSessionOpt = sessionRepository.findByDeliveryManIdAndStatus(deliveryManId, SessionStatus.IN_PROGRESS);

        DeliverySession sessionToUse;

        if (activeSessionOpt.isEmpty()) {
            // 4a. Logic: "scan đơn đầu tiên sẽ tạo phiên"
            log.info("No active session found for shipper {}. Creating a new session.", deliveryManId);
            sessionToUse = DeliverySession.builder()
                .deliveryManId(deliveryManId)
                .status(SessionStatus.IN_PROGRESS)
                .startTime(LocalDateTime.now())
                .build();
        } else {
            // 4b. Logic: "scan mới thì add thêm task vào phiên"
            log.info("Found active session {} for shipper {}. Adding task.", activeSessionOpt.get().getId(), deliveryManId);
            sessionToUse = activeSessionOpt.get();
        }

        // 5. Tạo Assignment (Task) mới
        DeliveryAssignment newAssignment = DeliveryAssignment.builder()
            .parcelId(parcelId)
            .status(AssignmentStatus.IN_PROGRESS)
            .scanedAt(LocalDateTime.now())
            .build();
        
        // 6. Liên kết task vào session
        sessionToUse.addAssignment(newAssignment);

        // 7. Gọi Parcel-Service để cập nhật trạng thái đơn hàng
        try {
            parcelApiClient.changeParcelStatus(parcelId, ParcelEvent.SCAN_QR);
        } catch (Exception e) {
            log.error("Failed to call Parcel-Service for parcel {}: {}", parcelId, e.getMessage());
            throw new RuntimeException("Failed to update parcel status via API: " + e.getMessage(), e);
        }

        // 8. Lưu session (và task mới sẽ được lưu theo nhờ CascadeType.ALL)
        sessionRepository.save(sessionToUse);
        log.info("Successfully added parcel {} to session {}.", parcelId, sessionToUse.getId());

        // Trả về task vừa được tạo
        return toAssignmentResponse(newAssignment);
    }


    @Override
    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        log.info("Creating new session for delivery man: {}", request.getDeliveryManId());

        // 1. Kiểm tra xem shipper đã có phiên IN_PROGRESS nào chưa
        sessionRepository.findByDeliveryManIdAndStatus(request.getDeliveryManId(), SessionStatus.IN_PROGRESS)
            .ifPresent(session -> {
                throw new IllegalStateException("Delivery man " + request.getDeliveryManId() + " already has an active session.");
            });
            
        // 2. Tạo Session cha
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
                parcelApiClient.changeParcelStatus(parcelId, ParcelEvent.SCAN_QR);
            } catch (Exception e) {
                // Xử lý nếu gọi API thất bại (ví dụ: dùng @Retryable)
                log.error("Failed to call Parcel-Service for parcel {}: {}", parcelId, e.getMessage());
                throw new RuntimeException("Failed to update parcel status via API: " + e.getMessage(), e);
            }
        }
        
        // 5. Lưu Session (và các Assignment con nhờ CascadeType.ALL)
        DeliverySession savedSession = sessionRepository.save(session);
        log.info("Session {} created with {} tasks.", savedSession.getId(), savedSession.getAssignments().size());

        return toSessionResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionResponse completeSession(UUID sessionId) {
        log.info("Completing session {}", sessionId);
        DeliverySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFound("Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            log.warn("Session {} is already in state {}. No action taken.", sessionId, session.getStatus());
            return toSessionResponse(session);
        }
        
        // Kiểm tra lại (phòng hờ): Đảm bảo tất cả task đã xong
        long pendingTasks = assignmentRepository.countBySession_IdAndStatus(session.getId(), AssignmentStatus.IN_PROGRESS);
        if (pendingTasks > 0) {
            throw new IllegalStateException("Cannot complete session " + sessionId + ": " + pendingTasks + " tasks are still PENDING.");
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setEndTime(LocalDateTime.now());
        DeliverySession savedSession = sessionRepository.save(session);
        
        // Xử lý các task DELAYED
        // Báo cho Parcel-Service chuyển các đơn DELAYED về IN_WAREHOUSE
        savedSession.getAssignments().stream()
            .filter(
                a -> a.getStatus().equals(AssignmentStatus.FAILED) && 
                (a.getFailReason().contains("hẹn") || a.getFailReason().contains("hoãn")))
            .forEach(da-> {
                try {
                    parcelApiClient.changeParcelStatus(da.getParcelId(), ParcelEvent.END_SESSION);
                } catch (Exception e) {
                    log.error("Failed to call Parcel-Service for parcel {}: {}", da.getParcelId(), e.getMessage());
                }
            });
        
        return toSessionResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionResponse failSession(UUID sessionId, String reason) {
        log.warn("Failing session {} due to: {}", sessionId, reason);
        DeliverySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFound("Session not found: " + sessionId));
        
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            log.warn("Session {} is already in state {}. No action taken.", sessionId, session.getStatus());
            return toSessionResponse(session);
        }
        
        session.setStatus(SessionStatus.FAILED);
        session.setEndTime(LocalDateTime.now());
        
        DeliverySession savedSession = sessionRepository.save(session);

        // 3. Xử lý tất cả các task còn lại
        List<DeliveryAssignment> pendingTasks = assignmentRepository.findBySession_IdAndStatus(sessionId, AssignmentStatus.IN_PROGRESS);
        for (DeliveryAssignment task : pendingTasks) {
            // Chuyển trạng thái task nội bộ
            task.setStatus(AssignmentStatus.FAILED);
            task.setFailReason("Session Failed: " + reason);
            assignmentRepository.save(task);

            // Báo cho Parcel-Service: k thể giao
            try {
                parcelApiClient.changeParcelStatus(task.getParcelId(), ParcelEvent.CAN_NOT_DELIVERY);
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
        return toSessionResponse(session);
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
}

