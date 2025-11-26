package com.ds.session.session_service.infrastructure.kafka;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.ds.session.session_service.common.entities.dto.event.ParcelStatusRequestEvent;
import com.ds.session.session_service.common.entities.dto.event.AssignmentCompletedEvent;
import com.ds.session.session_service.common.entities.dto.event.ParcelPostponedEvent;
import com.ds.session.session_service.common.entities.dto.event.SessionCompletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish a parcel status request event. Key should be parcelId to preserve ordering.
     */
    public CompletableFuture<SendResult<String, Object>> publishParcelStatusRequest(
            String parcelId, ParcelStatusRequestEvent payload) {

        log.debug("[session-service] [EventProducer.publish] Publishing parcel status request to topic: {}, parcelId={}", KafkaConfig.TOPIC_PARCEL_STATUS_REQUEST, parcelId);
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaConfig.TOPIC_PARCEL_STATUS_REQUEST, parcelId, payload);

        future.whenComplete((res, ex) -> {
            if (ex == null) {
                log.debug("Parcel status request published parcelId={}", parcelId);
            } else {
                log.error("[session-service] [EventProducer.publish] Failed to publish parcel status request parcelId={}", parcelId, ex);
            }
        });

        return future;
    }

    /**
     * Backward-compatible raw publish (useful for Outbox string payloads)
     */
    public CompletableFuture<SendResult<String, Object>> publishParcelStatusRequestRaw(
            String parcelId, Object rawPayload) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaConfig.TOPIC_PARCEL_STATUS_REQUEST, parcelId, rawPayload);
        future.whenComplete((res, ex) -> {
            if (ex == null) log.debug("Raw parcel status request published parcelId={}", parcelId);
            else log.error("[session-service] [EventProducer.publish] Failed to publish raw parcel status request parcelId={}", parcelId, ex);
        });
        return future;
    }

    /**
     * Publish inside a Kafka transaction. Useful when producing and then relying on transactions
     * for exactly-once processing (e.g., produce to topic and commit offsets atomically).
     */
    public void publishParcelStatusRequestInTransaction(String parcelId, ParcelStatusRequestEvent payload) {
        kafkaTemplate.executeInTransaction(k -> {
            k.send(KafkaConfig.TOPIC_PARCEL_STATUS_REQUEST, parcelId, payload);
            return null;
        });
    }
    
    /**
     * Publish assignment completed event to communication-service
     * This event triggers notifications for both shipper and client
     */
    public CompletableFuture<SendResult<String, Object>> publishAssignmentCompleted(
            String parcelId, AssignmentCompletedEvent event) {
        
        log.debug("[session-service] [EventProducer.publishAssignmentCompleted] Publishing assignment completed event to topic: {}, parcelId={}, assignmentId={}", 
                KafkaConfig.TOPIC_ASSIGNMENT_COMPLETED, parcelId, event.getAssignmentId());
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaConfig.TOPIC_ASSIGNMENT_COMPLETED, parcelId, event);
        
        future.whenComplete((res, ex) -> {
            if (ex == null) {
                log.debug("[session-service] [EventProducer.publishAssignmentCompleted] Assignment completed event published successfully: parcelId={}, assignmentId={}", 
                        parcelId, event.getAssignmentId());
            } else {
                log.error("[session-service] [EventProducer.publishAssignmentCompleted] Failed to publish assignment completed event: parcelId={}, assignmentId={}", 
                        parcelId, event.getAssignmentId(), ex);
            }
        });
        
        return future;
    }
    
    /**
     * Publish parcel postponed event to communication-service
     * This event triggers message to user when parcel is postponed (out of session)
     */
    public CompletableFuture<SendResult<String, Object>> publishParcelPostponed(
            String parcelId, ParcelPostponedEvent event) {
        
        log.debug("[session-service] [EventProducer.publishParcelPostponed] Publishing parcel postponed event to topic: {}, parcelId={}, assignmentId={}", 
                KafkaConfig.TOPIC_PARCEL_POSTPONED, parcelId, event.getAssignmentId());
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaConfig.TOPIC_PARCEL_POSTPONED, parcelId, event);
        
        future.whenComplete((res, ex) -> {
            if (ex == null) {
                log.debug("[session-service] [EventProducer.publishParcelPostponed] Parcel postponed event published successfully: parcelId={}, assignmentId={}", 
                        parcelId, event.getAssignmentId());
            } else {
                log.error("[session-service] [EventProducer.publishParcelPostponed] Failed to publish parcel postponed event: parcelId={}, assignmentId={}", 
                        parcelId, event.getAssignmentId(), ex);
            }
        });
        
        return future;
    }
    
    /**
     * Publish session completed event to communication-service
     * This event triggers notifications to all related clients/shippers
     */
    public CompletableFuture<SendResult<String, Object>> publishSessionCompleted(
            String sessionId, SessionCompletedEvent event) {
        
        log.debug("[session-service] [EventProducer.publishSessionCompleted] Publishing session completed event to topic: {}, sessionId={}", 
                KafkaConfig.TOPIC_SESSION_COMPLETED, sessionId);
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaConfig.TOPIC_SESSION_COMPLETED, sessionId, event);
        
        future.whenComplete((res, ex) -> {
            if (ex == null) {
                log.debug("[session-service] [EventProducer.publishSessionCompleted] Session completed event published successfully: sessionId={}", sessionId);
            } else {
                log.error("[session-service] [EventProducer.publishSessionCompleted] Failed to publish session completed event: sessionId={}", 
                        sessionId, ex);
            }
        });
        
        return future;
    }
}
