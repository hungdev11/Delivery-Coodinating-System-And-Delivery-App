package com.ds.session.session_service.infrastructure.kafka;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.ds.session.session_service.common.entities.dto.event.ParcelStatusRequestEvent;

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

        log.info("Publishing parcel status request to topic: {}, parcelId={}", KafkaConfig.TOPIC_PARCEL_STATUS_REQUEST, parcelId);
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaConfig.TOPIC_PARCEL_STATUS_REQUEST, parcelId, payload);

        future.whenComplete((res, ex) -> {
            if (ex == null) {
                log.debug("Parcel status request published parcelId={}", parcelId);
            } else {
                log.error("Failed to publish parcel status request parcelId={}: {}", parcelId, ex.getMessage(), ex);
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
            else log.error("Failed to publish raw parcel status request parcelId={}: {}", parcelId, ex.getMessage(), ex);
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
}
