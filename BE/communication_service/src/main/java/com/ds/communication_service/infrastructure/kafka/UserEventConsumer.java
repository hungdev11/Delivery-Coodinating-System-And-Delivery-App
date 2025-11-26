package com.ds.communication_service.infrastructure.kafka;

import com.ds.communication_service.app_context.models.UserSnapshot;
import com.ds.communication_service.app_context.repositories.UserSnapshotRepository;
import com.ds.communication_service.infrastructure.kafka.dto.UserEventDto;
import com.ds.communication_service.infrastructure.snapshot.SnapshotInitializationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumer for User events
 * Updates UserSnapshot table when user events are received
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final UserSnapshotRepository userSnapshotRepository;
    private final SnapshotInitializationService snapshotInitializationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "user-events",
        groupId = "${spring.kafka.consumer.group-id:communication-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleUserEvent(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String userId,
            Acknowledgment acknowledgment) {
        
        try {
            // Convert payload to UserEventDto
            UserEventDto event;
            if (payload instanceof UserEventDto) {
                event = (UserEventDto) payload;
            } else if (payload instanceof java.util.Map) {
                // Deserializer returned a Map, convert to DTO
                try {
                    event = objectMapper.convertValue(payload, UserEventDto.class);
                } catch (Exception e) {
                    log.error("[communication-service] [UserEventConsumer.handleUserEvent] Failed to convert payload to UserEventDto", e);
                    // Acknowledge to skip this message and avoid reprocessing
                    if (acknowledgment != null) {
                        acknowledgment.acknowledge();
                    }
                    return;
                }
            } else if (payload instanceof ConsumerRecord) {
                // Deserializer failed, received raw ConsumerRecord - extract value
                @SuppressWarnings("unchecked")
                ConsumerRecord<String, Object> record = (ConsumerRecord<String, Object>) payload;
                Object recordValue = record.value();
                
                if (recordValue instanceof java.util.Map) {
                    try {
                        event = objectMapper.convertValue(recordValue, UserEventDto.class);
                        log.debug("[communication-service] [UserEventConsumer.handleUserEvent] Extracted UserEventDto from ConsumerRecord");
                    } catch (Exception e) {
                        log.error("[communication-service] [UserEventConsumer.handleUserEvent] Failed to convert ConsumerRecord value to UserEventDto", e);
                        // Acknowledge to skip this message and avoid reprocessing
                        if (acknowledgment != null) {
                            acknowledgment.acknowledge();
                        }
                        return;
                    }
                } else {
                    log.error("[communication-service] [UserEventConsumer.handleUserEvent] ConsumerRecord value is not a Map: {}", 
                        recordValue != null ? recordValue.getClass().getName() : "null");
                    // Acknowledge to skip this message and avoid reprocessing
                    if (acknowledgment != null) {
                        acknowledgment.acknowledge();
                    }
                    return;
                }
            } else {
                log.error("[communication-service] [UserEventConsumer.handleUserEvent] Received unexpected message type in user-events topic: {}. Expected UserEventDto.", 
                    payload != null ? payload.getClass().getName() : "null");
                // Acknowledge to skip this message and avoid reprocessing
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }
            
            log.debug("[communication-service] [UserEventConsumer.handleUserEvent] Received user event: {} for user: {}", event.getEventType(), userId);

            switch (event.getEventType()) {
                case USER_CREATED:
                case USER_UPDATED:
                    handleUserCreatedOrUpdated(event);
                    break;
                case USER_DELETED:
                    handleUserDeleted(userId);
                    break;
                case USER_SERVICE_READY:
                    handleUserServiceReady();
                    break;
                default:
                    log.debug("[communication-service] [UserEventConsumer.handleUserEvent] Unknown event type: {}", event.getEventType());
            }

            // Acknowledge message after successful processing
            acknowledgment.acknowledge();
            log.debug("[communication-service] [UserEventConsumer.handleUserEvent] User event processed successfully: {} for user: {}", event.getEventType(), userId);

        } catch (Exception e) {
            log.error("[communication-service] [UserEventConsumer.handleUserEvent] Error processing user event for user: {}", 
                userId, e);
            // Don't acknowledge on error - message will be retried
            throw e;
        }
    }

    private void handleUserCreatedOrUpdated(UserEventDto event) {
        String userId = event.getUserId();
        if (userId == null || userId.isBlank()) {
            log.debug("[communication-service] [UserEventConsumer.handleUserCreatedOrUpdated] UserEventDto has no userId. Skipping.");
            return;
        }
        
        UserSnapshot snapshot = userSnapshotRepository.findById(userId)
            .orElse(UserSnapshot.builder()
                .userId(userId)
                .build());

        snapshot.setUsername(event.getUsername());
        snapshot.setFirstName(event.getFirstName());
        snapshot.setLastName(event.getLastName());
        snapshot.setEmail(event.getEmail());
        snapshot.setPhone(event.getPhone());
        snapshot.setAddress(event.getAddress());
        snapshot.setIdentityNumber(event.getIdentityNumber());
        snapshot.setStatus(event.getStatus());

        userSnapshotRepository.save(snapshot);
        log.debug("[communication-service] [UserEventConsumer.handleUserCreatedOrUpdated] User snapshot updated: {}", userId);
    }

    private void handleUserDeleted(String userId) {
        if (userId == null || userId.isBlank()) {
            log.debug("[communication-service] [UserEventConsumer.handleUserDeleted] Cannot delete user snapshot: userId is null or blank.");
            return;
        }
        userSnapshotRepository.deleteById(userId);
        log.debug("[communication-service] [UserEventConsumer.handleUserDeleted] User snapshot deleted: {}", userId);
    }

    private void handleUserServiceReady() {
        log.debug("[communication-service] [UserEventConsumer.handleUserServiceReady] Received USER_SERVICE_READY event. Triggering snapshot synchronization...");
        try {
            // Trigger snapshot initialization if table is empty
            long snapshotCount = userSnapshotRepository.count();
            if (snapshotCount == 0) {
                log.debug("[communication-service] [UserEventConsumer.handleUserServiceReady] Snapshot table is empty. Starting sync from UserService...");
                snapshotInitializationService.doInitializeSnapshot();
            } else {
                log.debug("[communication-service] [UserEventConsumer.handleUserServiceReady] Snapshot table already has {} records. Skipping sync.", snapshotCount);
            }
        } catch (Exception e) {
            log.error("[communication-service] [UserEventConsumer.handleUserServiceReady] Error during snapshot sync after USER_SERVICE_READY", e);
        }
    }
}
