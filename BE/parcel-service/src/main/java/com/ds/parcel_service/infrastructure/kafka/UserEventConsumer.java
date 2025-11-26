package com.ds.parcel_service.infrastructure.kafka;

import com.ds.parcel_service.app_context.models.UserSnapshot;
import com.ds.parcel_service.app_context.repositories.UserSnapshotRepository;
import com.ds.parcel_service.infrastructure.kafka.dto.UserEventDto;
import com.ds.parcel_service.infrastructure.snapshot.SnapshotInitializationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @KafkaListener(
        topics = "user-events",
        groupId = "${spring.kafka.consumer.group-id:parcel-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleUserEvent(
            @Payload UserEventDto event,
            @Header(KafkaHeaders.RECEIVED_KEY) String userId,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("[parcel-service] [UserEventConsumer.handleUserEvent] Received user event: {} for user: {}", event.getEventType(), userId);

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
                    log.debug("[parcel-service] [UserEventConsumer.handleUserEvent] Unknown event type: {}", event.getEventType());
            }

            // Acknowledge message after successful processing
            acknowledgment.acknowledge();
            log.debug("[parcel-service] [UserEventConsumer.handleUserEvent] User event processed successfully: {} for user: {}", event.getEventType(), userId);

        } catch (Exception e) {
            log.error("[parcel-service] [UserEventConsumer.handleUserEvent] Error processing user event: {} for user: {}", 
                event.getEventType(), userId, e);
            // Don't acknowledge on error - message will be retried
            throw e;
        }
    }

    private void handleUserCreatedOrUpdated(UserEventDto event) {
        UserSnapshot snapshot = userSnapshotRepository.findById(event.getUserId())
            .orElse(UserSnapshot.builder()
                .userId(event.getUserId())
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
        log.debug("[parcel-service] [UserEventConsumer.handleUserCreatedOrUpdated] User snapshot updated: {}", event.getUserId());
    }

    private void handleUserDeleted(String userId) {
        userSnapshotRepository.deleteById(userId);
        log.debug("[parcel-service] [UserEventConsumer.handleUserDeleted] User snapshot deleted: {}", userId);
    }

    private void handleUserServiceReady() {
        log.debug("[parcel-service] [UserEventConsumer.handleUserServiceReady] Received USER_SERVICE_READY event. Triggering snapshot synchronization...");
        try {
            // Trigger snapshot initialization if table is empty
            long snapshotCount = userSnapshotRepository.count();
            if (snapshotCount == 0) {
                log.debug("[parcel-service] [UserEventConsumer.handleUserServiceReady] Snapshot table is empty. Starting sync from UserService...");
                snapshotInitializationService.doInitializeSnapshot();
            } else {
                log.debug("[parcel-service] [UserEventConsumer.handleUserServiceReady] Snapshot table already has {} records. Skipping sync.", snapshotCount);
            }
        } catch (Exception e) {
            log.error("[parcel-service] [UserEventConsumer.handleUserServiceReady] Error during snapshot sync after USER_SERVICE_READY", e);
        }
    }
}
