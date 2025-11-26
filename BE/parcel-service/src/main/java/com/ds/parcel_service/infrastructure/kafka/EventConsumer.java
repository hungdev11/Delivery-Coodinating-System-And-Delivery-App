package com.ds.parcel_service.infrastructure.kafka;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer for parcel status requests. Uses manual ack and is intended to participate in Kafka transactions
 * (producer is transactional) so that processing can achieve end-to-end exactly-once semantics when used with
 * transaction-aware production of outgoing events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    // Inject ParcelService to apply state changes
    private final com.ds.parcel_service.common.interfaces.IParcelService parcelService;

    @KafkaListener(
        topics = KafkaConfig.TOPIC_PARCEL_STATUS_REQUEST,
        groupId = "${spring.kafka.consumer.group-id:parcel-service-group}",
        containerFactory = "parcelStatusRequestListenerContainerFactory"
    )
    public void consumeParcelStatusRequest(
        @Payload com.ds.parcel_service.common.events.ParcelStatusRequestEvent payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.debug("[parcel-service] [EventConsumer.consumeParcelStatusRequest] Received parcel status request from Kafka. topic={}, partition={}, offset={}", topic, partition, offset);

            // typed payload -> direct access to fields
            log.debug("payload={}", payload);

            if (payload == null || payload.getParcelId() == null || payload.getEventType() == null) {
                log.debug("[parcel-service] [EventConsumer.consumeParcelStatusRequest] Invalid parcel status event payload, missing parcelId or eventType. Payload={}", payload);
                // acknowledge to avoid poison looping, or you may choose to send to DLQ
                if (acknowledgment != null) acknowledgment.acknowledge();
                return;
            }

            try {
                UUID parcelId = payload.getParcelId();
                com.ds.parcel_service.common.enums.ParcelEvent pe = com.ds.parcel_service.common.enums.ParcelEvent.valueOf(payload.getEventType());
                
                // Check current parcel status before applying event to handle duplicate events gracefully
                try {
                    com.ds.parcel_service.common.entities.dto.response.ParcelResponse currentParcel = parcelService.getParcelById(parcelId);
                    if (currentParcel != null && currentParcel.getStatus() != null) {
                        com.ds.parcel_service.common.enums.ParcelStatus currentStatus = currentParcel.getStatus();
                        
                        // Handle duplicate DELIVERY_SUCCESSFUL event: if parcel is already DELIVERED or beyond, acknowledge and skip
                        if (pe == com.ds.parcel_service.common.enums.ParcelEvent.DELIVERY_SUCCESSFUL) {
                            if (currentStatus == com.ds.parcel_service.common.enums.ParcelStatus.DELIVERED || 
                                currentStatus == com.ds.parcel_service.common.enums.ParcelStatus.SUCCEEDED ||
                                currentStatus == com.ds.parcel_service.common.enums.ParcelStatus.FAILED ||
                                currentStatus == com.ds.parcel_service.common.enums.ParcelStatus.DISPUTE) {
                                log.debug("[parcel-service] [EventConsumer.consumeParcelStatusRequest] Parcel {} is already in state {} (DELIVERED or beyond). Skipping duplicate DELIVERY_SUCCESSFUL event.", 
                                        parcelId, currentStatus);
                                // Acknowledge to prevent infinite retries
                                if (acknowledgment != null) {
                                    acknowledgment.acknowledge();
                                }
                                return;
                            }
                        }
                    }
                } catch (Exception statusCheckEx) {
                    log.debug("[parcel-service] [EventConsumer.consumeParcelStatusRequest] Failed to check current parcel status for parcelId={}: {}. Proceeding with event application.", 
                            parcelId, statusCheckEx.getMessage());
                    // Continue with event application even if status check fails
                }
                
                parcelService.changeParcelStatus(parcelId, pe);

                // After successful processing (business logic applied), acknowledge the offset
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
            } catch (IllegalStateException ex) {
                // Handle invalid state transitions gracefully
                String errorMsg = ex.getMessage();
                if (errorMsg != null && errorMsg.contains("Invalid event")) {
                    log.debug("[parcel-service] [EventConsumer.consumeParcelStatusRequest] Invalid state transition for parcelId={} event={}: {}. Acknowledging to prevent infinite retries.", 
                            payload.getParcelId(), payload.getEventType(), errorMsg);
                    // Acknowledge invalid transitions to prevent infinite retries
                    if (acknowledgment != null) {
                        acknowledgment.acknowledge();
                    }
                } else {
                    log.error("[parcel-service] [EventConsumer.consumeParcelStatusRequest] Failed to apply parcel event for parcelId={} event={}", 
                            payload.getParcelId(), payload.getEventType(), ex);
                    // don't ack so message can be retried according to container retry policy
                }
            } catch (Exception ex) {
                log.error("[parcel-service] [EventConsumer.consumeParcelStatusRequest] Failed to apply parcel event for parcelId={} event={}", 
                        payload.getParcelId(), payload.getEventType(), ex);
                // don't ack so message can be retried according to container retry policy
            }

        } catch (Exception ex) {
            log.error("[parcel-service] [EventConsumer.consumeParcelStatusRequest] Error processing parcel status request", ex);
            // don't ack so message can be retried according to container retry policy
        }
    }
}
