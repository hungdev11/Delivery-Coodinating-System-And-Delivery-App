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
            log.info("Received parcel status request from Kafka. topic={}, partition={}, offset={}", topic, partition, offset);

            // typed payload -> direct access to fields
            log.debug("payload={}", payload);

            if (payload == null || payload.getParcelId() == null || payload.getEventType() == null) {
                log.warn("Invalid parcel status event payload, missing parcelId or eventType. Payload={}", payload);
                // acknowledge to avoid poison looping, or you may choose to send to DLQ
                if (acknowledgment != null) acknowledgment.acknowledge();
                return;
            }

            try {
                UUID parcelId = payload.getParcelId();
                com.ds.parcel_service.common.enums.ParcelEvent pe = com.ds.parcel_service.common.enums.ParcelEvent.valueOf(payload.getEventType());
                parcelService.changeParcelStatus(parcelId, pe);

                // After successful processing (business logic applied), acknowledge the offset
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
            } catch (Exception ex) {
                log.error("Failed to apply parcel event for parcelId={} event={}: {}", payload.getParcelId(), payload.getEventType(), ex.getMessage(), ex);
                // don't ack so message can be retried according to container retry policy
            }

        } catch (Exception ex) {
            log.error("Error processing parcel status request: {}", ex.getMessage(), ex);
            // don't ack so message can be retried according to container retry policy
        }
    }
}
