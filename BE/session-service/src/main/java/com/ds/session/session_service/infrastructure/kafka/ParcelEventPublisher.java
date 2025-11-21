package com.ds.session.session_service.infrastructure.kafka;

import org.springframework.stereotype.Service;

import com.ds.session.session_service.common.entities.dto.event.ParcelStatusRequestEvent;
import com.ds.session.session_service.common.enums.ParcelEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParcelEventPublisher {

    private final EventProducer producer;

    /**
     * Build a ParcelStatusRequestEvent and publish it inside a Kafka transaction.
     */
    public void publish(String parcelId, ParcelEvent event) {
        ParcelStatusRequestEvent ev = ParcelStatusRequestEvent.builder()
            .eventId(java.util.UUID.randomUUID().toString())
            .parcelId(parcelId)
            .sourceService("session-service")
            .eventType(event.name())
            .createdAt(java.time.Instant.now())
            .build();

        producer.publishParcelStatusRequestInTransaction(parcelId, ev);
    }
}
