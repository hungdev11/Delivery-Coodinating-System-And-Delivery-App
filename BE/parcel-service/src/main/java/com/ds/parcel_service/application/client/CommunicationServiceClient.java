package com.ds.parcel_service.application.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "communication-service", url = "${services.communication.base-url}")
public interface CommunicationServiceClient {

    /**
     * Create a NOT_RECEIVED ticket when client reports not receiving parcel
     */
    @PostMapping("/api/v1/tickets/not-received")
    void createNotReceivedTicket(@RequestBody CreateNotReceivedTicketRequest request);

    /**
     * Request DTO for creating not received ticket
     */
    record CreateNotReceivedTicketRequest(
        String parcelId,
        String deliveryAssignmentId,
        String clientId,
        String description
    ) {}
}
