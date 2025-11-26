package com.ds.session.session_service.infrastructure.kafka;

import com.ds.session.session_service.app_context.models.DeliveryManSnapshot;
import com.ds.session.session_service.app_context.models.UserSnapshot;
import com.ds.session.session_service.app_context.repositories.DeliveryManSnapshotRepository;
import com.ds.session.session_service.app_context.repositories.UserSnapshotRepository;
import com.ds.session.session_service.infrastructure.kafka.dto.UserEventDto;
import com.ds.session.session_service.infrastructure.snapshot.SnapshotInitializationService;
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
 * Updates UserSnapshot and DeliveryManSnapshot tables when user events are received
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final UserSnapshotRepository userSnapshotRepository;
    private final DeliveryManSnapshotRepository deliveryManSnapshotRepository;
    private final SnapshotInitializationService snapshotInitializationService;

    @KafkaListener(
        topics = "user-events",
        groupId = "${spring.kafka.consumer.group-id:session-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleUserEvent(
            @Payload UserEventDto event,
            @Header(KafkaHeaders.RECEIVED_KEY) String userId,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("[session-service] [UserEventConsumer.handleUserEvent] Received user event: {} for user: {}", event.getEventType(), userId);

            switch (event.getEventType()) {
                case USER_CREATED:
                case USER_UPDATED:
                    handleUserCreatedOrUpdated(event);
                    break;
                case USER_DELETED:
                    handleUserDeleted(userId);
                    break;
                case DELIVERY_MAN_CREATED:
                case DELIVERY_MAN_UPDATED:
                    handleDeliveryManCreatedOrUpdated(event);
                    break;
                case DELIVERY_MAN_DELETED:
                    handleDeliveryManDeleted(userId);
                    break;
                case USER_SERVICE_READY:
                    snapshotInitializationService.doInitializeSnapshot();
                    break;
                default:
                    log.debug("[session-service] [UserEventConsumer.handleUserEvent] Unknown event type: {}", event.getEventType());
            }

            // Acknowledge message after successful processing
            acknowledgment.acknowledge();
            log.debug("[session-service] [UserEventConsumer.handleUserEvent] User event processed successfully: {} for user: {}", event.getEventType(), userId);

        } catch (Exception e) {
            log.error("[session-service] [UserEventConsumer.handleUserEvent] Error processing user event: {} for user: {}: {}", 
                event.getEventType(), userId, e.getMessage(), e);
            // Don't acknowledge on error - message will be retried
            throw e;
        }
    }

    private void handleUserCreatedOrUpdated(UserEventDto event) {
        // Update UserSnapshot
        UserSnapshot userSnapshot = userSnapshotRepository.findById(event.getUserId())
            .orElse(UserSnapshot.builder()
                .userId(event.getUserId())
                .build());

        userSnapshot.setUsername(event.getUsername());
        userSnapshot.setFirstName(event.getFirstName());
        userSnapshot.setLastName(event.getLastName());
        userSnapshot.setEmail(event.getEmail());
        userSnapshot.setPhone(event.getPhone());
        userSnapshot.setAddress(event.getAddress());
        userSnapshot.setIdentityNumber(event.getIdentityNumber());
        userSnapshot.setStatus(event.getStatus());

        userSnapshotRepository.save(userSnapshot);
        log.debug("[session-service] [UserEventConsumer.handleUserCreatedOrUpdated] User snapshot updated: {}", event.getUserId());

        // Update DeliveryManSnapshot if it exists
        // Note: DeliveryMan-specific fields (vehicleType, capacityKg) are updated separately
        // when DeliveryMan events are received (if we implement that)
        deliveryManSnapshotRepository.findById(event.getUserId())
            .ifPresent(dmSnapshot -> {
                dmSnapshot.setUsername(event.getUsername());
                dmSnapshot.setFirstName(event.getFirstName());
                dmSnapshot.setLastName(event.getLastName());
                dmSnapshot.setEmail(event.getEmail());
                dmSnapshot.setPhone(event.getPhone());
                deliveryManSnapshotRepository.save(dmSnapshot);
                log.debug("[session-service] [UserEventConsumer.handleUserCreatedOrUpdated] DeliveryMan snapshot updated: {}", event.getUserId());
            });
    }

    private void handleUserDeleted(String userId) {
        userSnapshotRepository.deleteById(userId);
        deliveryManSnapshotRepository.deleteById(userId);
        log.debug("[session-service] [UserEventConsumer.handleUserDeleted] User and DeliveryMan snapshots deleted: {}", userId);
    }

    private void handleDeliveryManCreatedOrUpdated(UserEventDto event) {
        // First, ensure UserSnapshot exists (update it if needed)
        handleUserCreatedOrUpdated(event);

        // Then update/create DeliveryManSnapshot
        DeliveryManSnapshot dmSnapshot = deliveryManSnapshotRepository.findById(event.getUserId())
            .orElse(DeliveryManSnapshot.builder()
                .userId(event.getUserId())
                .build());

        // Update user info
        dmSnapshot.setUsername(event.getUsername());
        dmSnapshot.setFirstName(event.getFirstName());
        dmSnapshot.setLastName(event.getLastName());
        dmSnapshot.setEmail(event.getEmail());
        dmSnapshot.setPhone(event.getPhone());

        // Update DeliveryMan-specific fields
        if (event.getVehicleType() != null) {
            dmSnapshot.setVehicleType(event.getVehicleType());
        }
        if (event.getCapacityKg() != null) {
            dmSnapshot.setCapacityKg(event.getCapacityKg());
        }

        deliveryManSnapshotRepository.save(dmSnapshot);
        log.debug("[session-service] [UserEventConsumer.handleDeliveryManCreatedOrUpdated] DeliveryMan snapshot created/updated: {}", event.getUserId());
    }

    private void handleDeliveryManDeleted(String userId) {
        deliveryManSnapshotRepository.deleteById(userId);
        log.debug("[session-service] [UserEventConsumer.handleDeliveryManDeleted] DeliveryMan snapshot deleted: {}", userId);
    }

    private void handleUserServiceReady() {
        log.debug("[session-service] [UserEventConsumer.handleUserServiceReady] Received USER_SERVICE_READY event. Triggering snapshot synchronization...");
        try {
            // Trigger snapshot initialization if tables are empty
            long userSnapshotCount = userSnapshotRepository.count();
            long deliveryManSnapshotCount = deliveryManSnapshotRepository.count();
            
            if (userSnapshotCount == 0 || deliveryManSnapshotCount == 0) {
                log.debug("[session-service] [UserEventConsumer.handleUserServiceReady] Snapshot tables are empty (users: {}, deliveryMen: {}). Starting sync from UserService...", 
                    userSnapshotCount, deliveryManSnapshotCount);
                snapshotInitializationService.doInitializeSnapshot();
            } else {
                log.debug("[session-service] [UserEventConsumer.handleUserServiceReady] Snapshot tables already have data (users: {}, deliveryMen: {}). Skipping sync.", 
                    userSnapshotCount, deliveryManSnapshotCount);
            }
        } catch (Exception e) {
            log.error("[session-service] [UserEventConsumer.handleUserServiceReady] Error during snapshot sync after USER_SERVICE_READY", e);
        }
    }
}
