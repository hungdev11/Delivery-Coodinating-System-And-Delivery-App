package com.ds.user.infrastructure.kafka;

import com.ds.user.infrastructure.kafka.dto.UserEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes USER_SERVICE_READY event when UserService startup completes
 * Other services can listen to this event to trigger snapshot synchronization
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceReadyPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void publishUserServiceReady() {
        log.debug("[user-service] [UserServiceReadyPublisher.publishUserServiceReady] UserService startup completed. Publishing USER_SERVICE_READY event...");
        
        UserEventDto event = UserEventDto.builder()
                .eventType(UserEventDto.EventType.USER_SERVICE_READY)
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        // Use "user-service-ready" as key for this special event
        String eventKey = "user-service-ready";
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(KafkaConfig.TOPIC_USER_EVENTS, eventKey, event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("[user-service] [UserServiceReadyPublisher.publishUserServiceReady] USER_SERVICE_READY event published successfully");
            } else {
                log.error("[user-service] [UserServiceReadyPublisher.publishUserServiceReady] Failed to publish USER_SERVICE_READY event", ex);
            }
        });
    }
}
