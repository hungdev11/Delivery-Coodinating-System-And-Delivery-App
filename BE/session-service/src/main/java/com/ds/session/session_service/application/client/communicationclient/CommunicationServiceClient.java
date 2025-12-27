package com.ds.session.session_service.application.client.communicationclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ds.session.session_service.common.entities.dto.event.LocationTrackingEvent;

@FeignClient(name = "communication-service", url = "${services.communication.base-url}")
public interface CommunicationServiceClient {

    /**
     * Send location tracking event to communication-service for WebSocket broadcast.
     */
    @PostMapping("/api/v1/location-tracking/events")
    void sendLocationTrackingEvent(@RequestBody LocationTrackingEvent event);
    
    /**
     * Create a ticket in communication-service
     * Used for automatic ticket creation when delivery fails or incidents occur
     */
    @PostMapping("/api/v1/tickets")
    void createTicket(
        @RequestBody CreateTicketRequest request,
        @RequestParam String reporterId
    );
    
    /**
     * Request DTO for creating a ticket
     */
    public static class CreateTicketRequest {
        private String type; // TicketType enum as string: "DELIVERY_FAILED" or "NOT_RECEIVED"
        private String parcelId;
        private String assignmentId;
        private String description;
        
        public CreateTicketRequest() {}
        
        public CreateTicketRequest(String type, String parcelId, String assignmentId, String description) {
            this.type = type;
            this.parcelId = parcelId;
            this.assignmentId = assignmentId;
            this.description = description;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getParcelId() { return parcelId; }
        public void setParcelId(String parcelId) { this.parcelId = parcelId; }
        
        public String getAssignmentId() { return assignmentId; }
        public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
