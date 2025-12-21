package com.ds.session.session_service.infrastructure.scheduler;

import com.ds.session.session_service.app_context.models.DeliverySession;
import com.ds.session.session_service.app_context.repositories.DeliverySessionRepository;
import com.ds.session.session_service.common.enums.SessionStatus;
import com.ds.session.session_service.common.interfaces.ISessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Scheduled task to automatically close sessions that have expired
 * Runs daily at 20:00 to close sessions that started between 8:00-18:00 but haven't been closed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionAutoCloseScheduler {
    
    private final ISessionService sessionService;
    private final DeliverySessionRepository sessionRepository;
    
    /**
     * Check and auto-close expired sessions
     * Runs at 20:00 every day
     */
    @Scheduled(cron = "0 0 20 * * ?") // 20:00:00 every day
    public void autoCloseExpiredSessions() {
        log.info("[session-service] [SessionAutoCloseScheduler] Starting auto-close expired sessions task");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Find sessions that:
        // 1. Started between 8:00-18:00 today
        // 2. Status is CREATED or IN_PROGRESS (not already closed)
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime eightAM = startOfDay.plusHours(8);
        LocalDateTime sixPM = startOfDay.plusHours(18);
        
        List<DeliverySession> activeSessions = sessionRepository.findByStartTimeBetweenAndStatusIn(
            eightAM,
            sixPM,
            List.of(SessionStatus.CREATED, SessionStatus.IN_PROGRESS)
        );
        
        log.info("[session-service] [SessionAutoCloseScheduler] Found {} active sessions to auto-close", activeSessions.size());
        
        int closedCount = 0;
        int failedCount = 0;
        
        for (DeliverySession session : activeSessions) {
            try {
                // Auto-close session (this will handle parcel status updates and publish events)
                sessionService.completeSession(session.getId());
                closedCount++;
                log.info("[session-service] [SessionAutoCloseScheduler] Auto-closed session: {} (deliveryManId: {})", 
                    session.getId(), session.getDeliveryManId());
            } catch (Exception e) {
                failedCount++;
                log.error("[session-service] [SessionAutoCloseScheduler] Failed to auto-close session: {} (deliveryManId: {}). Error: {}", 
                    session.getId(), session.getDeliveryManId(), e.getMessage(), e);
            }
        }
        
        log.info("[session-service] [SessionAutoCloseScheduler] Auto-close task completed: {} sessions closed, {} failed", 
            closedCount, failedCount);
    }
    
    /**
     * Health check - runs every minute to verify scheduler is working
     * This is just for monitoring, can be removed in production
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    public void healthCheck() {
        LocalTime now = LocalTime.now();
        // Only log around 20:00 to avoid spam
        if (now.getHour() == 20 && now.getMinute() < 5) {
            log.debug("[session-service] [SessionAutoCloseScheduler] Scheduler is active and running");
        }
    }
}
