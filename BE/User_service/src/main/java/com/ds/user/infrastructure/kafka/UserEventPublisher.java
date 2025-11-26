package com.ds.user.infrastructure.kafka;

import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.base.DeliveryMan;
import com.ds.user.infrastructure.kafka.dto.UserEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for User events
 * Publishes user lifecycle events (created, updated, deleted) for snapshot synchronization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish user created event
     */
    public void publishUserCreated(User user) {
        UserEventDto event = UserEventDto.builder()
                .eventType(UserEventDto.EventType.USER_CREATED)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .identityNumber(user.getIdentityNumber())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        publishEvent(user.getId(), event);
    }

    /**
     * Publish user updated event
     */
    public void publishUserUpdated(User user) {
        UserEventDto event = UserEventDto.builder()
                .eventType(UserEventDto.EventType.USER_UPDATED)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .identityNumber(user.getIdentityNumber())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        publishEvent(user.getId(), event);
    }

    /**
     * Publish user deleted event
     */
    public void publishUserDeleted(String userId) {
        UserEventDto event = UserEventDto.builder()
                .eventType(UserEventDto.EventType.USER_DELETED)
                .userId(userId)
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        publishEvent(userId, event);
    }

    /**
     * Publish delivery man created event
     */
    public void publishDeliveryManCreated(DeliveryMan deliveryMan, User user) {
        UserEventDto event = UserEventDto.builder()
                .eventType(UserEventDto.EventType.DELIVERY_MAN_CREATED)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .identityNumber(user.getIdentityNumber())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deliveryManId(deliveryMan.getId())
                .vehicleType(deliveryMan.getVehicleType())
                .capacityKg(deliveryMan.getCapacityKg())
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        publishEvent(user.getId(), event);
    }

    /**
     * Publish delivery man updated event
     */
    public void publishDeliveryManUpdated(DeliveryMan deliveryMan, User user) {
        UserEventDto event = UserEventDto.builder()
                .eventType(UserEventDto.EventType.DELIVERY_MAN_UPDATED)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .identityNumber(user.getIdentityNumber())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deliveryManId(deliveryMan.getId())
                .vehicleType(deliveryMan.getVehicleType())
                .capacityKg(deliveryMan.getCapacityKg())
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        publishEvent(user.getId(), event);
    }

    /**
     * Publish delivery man deleted event
     */
    public void publishDeliveryManDeleted(String userId) {
        UserEventDto event = UserEventDto.builder()
                .eventType(UserEventDto.EventType.DELIVERY_MAN_DELETED)
                .userId(userId)
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        publishEvent(userId, event);
    }

    /**
     * Publish user service ready event
     * Called when UserService startup completes
     */
    public void publishUserServiceReady() {
        UserEventDto event = UserEventDto.builder()
                .eventType(UserEventDto.EventType.USER_SERVICE_READY)
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        // Use "user-service-ready" as key for this special event
        publishEvent("user-service-ready", event);
    }

    private void publishEvent(String userId, UserEventDto event) {
        log.debug("[user-service] [UserEventPublisher.publishEvent] Publishing user event: {} for user: {}", event.getEventType(), userId);
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(KafkaConfig.TOPIC_USER_EVENTS, userId, event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("✅ User event published successfully: {} for user: {}", 
                    event.getEventType(), userId);
            } else {
                log.error("❌ Failed to publish user event: {} for user: {}: {}", 
                    event.getEventType(), userId, ex.getMessage());
            }
        });
    }
}
