package com.ds.parcel_service.infrastructure.kafka;

import com.ds.parcel_service.infrastructure.kafka.dto.UserEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for User events
 * Consumes user-events topic to stay synchronized with user data changes
 * Currently logs events for monitoring/debugging purposes
 * Can be extended to invalidate caches or trigger other actions as needed
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    @KafkaListener(
        topics = "user-events",
        groupId = "${spring.kafka.consumer.group-id:parcel-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserEvent(
            @Payload UserEventDto event,
            @Header(KafkaHeaders.RECEIVED_KEY) String userId,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("[parcel-service] [UserEventConsumer.handleUserEvent] Received user event: {} for user: {}", 
                event.getEventType(), userId);

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
                    handleUserServiceReady();
                    break;
                default:
                    log.debug("[parcel-service] [UserEventConsumer.handleUserEvent] Unknown event type: {}", event.getEventType());
            }

            // Acknowledge message after successful processing
            acknowledgment.acknowledge();
            log.debug("[parcel-service] [UserEventConsumer.handleUserEvent] User event processed successfully: {} for user: {}", 
                event.getEventType(), userId);

        } catch (Exception e) {
            log.error("[parcel-service] [UserEventConsumer.handleUserEvent] Error processing user event: {} for user: {}: {}", 
                event.getEventType(), userId, e.getMessage(), e);
            // Don't acknowledge on error - message will be retried
            throw e;
        }
    }

    private void handleUserCreatedOrUpdated(UserEventDto event) {
        log.debug("[parcel-service] [UserEventConsumer.handleUserCreatedOrUpdated] User created/updated: userId={}, username={}, email={}", 
            event.getUserId(), event.getUsername(), event.getEmail());
        
        // TODO: Invalidate any user-related caches if implemented
        // TODO: Trigger any dependent data refreshes if needed
    }

    private void handleUserDeleted(String userId) {
        log.debug("[parcel-service] [UserEventConsumer.handleUserDeleted] User deleted: userId={}", userId);
        
        // TODO: Handle user deletion - cleanup related data if needed
        // Note: Parcel service may need to handle soft-delete scenarios
    }

    private void handleDeliveryManCreatedOrUpdated(UserEventDto event) {
        log.debug("[parcel-service] [UserEventConsumer.handleDeliveryManCreatedOrUpdated] DeliveryMan created/updated: userId={}, username={}", 
            event.getUserId(), event.getUsername());
        
        // TODO: Invalidate delivery man related caches if implemented
        // TODO: Trigger any dependent data refreshes if needed
    }

    private void handleDeliveryManDeleted(String userId) {
        log.debug("[parcel-service] [UserEventConsumer.handleDeliveryManDeleted] DeliveryMan deleted: userId={}", userId);
        
        // TODO: Handle delivery man deletion - cleanup related data if needed
    }

    private void handleUserServiceReady() {
        log.info("[parcel-service] [UserEventConsumer.handleUserServiceReady] Received USER_SERVICE_READY event. User service is ready.");
        
        // TODO: Trigger any initialization or synchronization tasks if needed
        // For example, sync user data if parcel-service maintains any user cache
    }
}
