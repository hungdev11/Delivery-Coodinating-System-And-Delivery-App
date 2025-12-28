package com.ds.session.session_service.application.client.communicationclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ds.session.session_service.common.entities.dto.event.LocationTrackingEvent;

@FeignClient(name = "communication-service", url = "${services.communication.base-url}")
public interface CommunicationServiceClient {

    /**
     * Send location tracking event to communication-service for WebSocket broadcast.
     */
    @PostMapping("/api/v1/location-tracking/events")
    void sendLocationTrackingEvent(@RequestBody LocationTrackingEvent event);

    /**
     * Create a DELIVERY_FAILED ticket when shipper reports delivery failure
     */
    @PostMapping("/api/v1/tickets/delivery-failed")
    void createDeliveryFailedTicket(@RequestBody CreateDeliveryFailedTicketRequest request);

    /**
     * Request DTO for creating delivery failed ticket
     */
    record CreateDeliveryFailedTicketRequest(
        String parcelId,
        String deliveryAssignmentId,
        String shipperId,
        String description
    ) {}
}
