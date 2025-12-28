package com.ds.communication_service.application.controller;

import com.ds.communication_service.business.v1.services.TicketService;
import com.ds.communication_service.common.dto.BaseResponse;
import com.ds.communication_service.common.dto.CreateTicketRequest;
import com.ds.communication_service.common.dto.ReassignParcelRequest;
import com.ds.communication_service.common.dto.TicketResponse;
import com.ds.communication_service.common.dto.UpdateTicketRequest;
import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for managing tickets
 * Provides endpoints for creating, querying, and resolving delivery issue tickets
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;

    /**
     * Create a new ticket
     * Can be called by shipper (DELIVERY_FAILED) or client (NOT_RECEIVED)
     */
    @PostMapping
    public ResponseEntity<BaseResponse<TicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        log.debug("Create ticket: parcelId={}, type={}, userId={}", 
            request.getParcelId(), request.getType(), userId);
        
        // Ensure userId in request matches the authenticated user
        if (!request.getUserId().equals(userId)) {
            throw new SecurityException("User ID mismatch");
        }
        
        TicketResponse ticket = ticketService.createTicket(request);
        return ResponseEntity.ok(BaseResponse.success(ticket));
    }

    /**
     * Get ticket by ID
     */
    @GetMapping("/{ticketId}")
    public ResponseEntity<BaseResponse<TicketResponse>> getTicket(
            @PathVariable String ticketId,
            @RequestHeader("X-User-Id") String userId) {
        
        log.debug("Get ticket: ticketId={}, userId={}", ticketId, userId);
        TicketResponse ticket = ticketService.getTicket(ticketId);
        
        // Security check: user can only view their own tickets (unless admin)
        // Note: Admin check should be done via role-based authorization in gateway
        if (!ticket.getUserId().equals(userId)) {
            throw new SecurityException("User does not have access to this ticket");
        }
        
        return ResponseEntity.ok(BaseResponse.success(ticket));
    }

    /**
     * Get all tickets (paginated) - Admin only
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<TicketResponse>>> getAllTickets(
            @RequestHeader("X-User-Id") String userId,
            Pageable pageable) {
        
        log.debug("Get all tickets: userId={}", userId);
        Page<TicketResponse> tickets = ticketService.getAllTickets(pageable);
        return ResponseEntity.ok(BaseResponse.success(tickets));
    }

    /**
     * Get tickets by current user (paginated)
     */
    @GetMapping("/my-tickets")
    public ResponseEntity<BaseResponse<Page<TicketResponse>>> getMyTickets(
            @RequestHeader("X-User-Id") String userId,
            Pageable pageable) {
        
        log.debug("Get my tickets: userId={}", userId);
        Page<TicketResponse> tickets = ticketService.getTicketsByUser(userId, pageable);
        return ResponseEntity.ok(BaseResponse.success(tickets));
    }

    /**
     * Get tickets by parcel ID
     */
    @GetMapping("/parcel/{parcelId}")
    public ResponseEntity<BaseResponse<List<TicketResponse>>> getTicketsByParcel(
            @PathVariable String parcelId) {
        
        log.debug("Get tickets by parcel: parcelId={}", parcelId);
        List<TicketResponse> tickets = ticketService.getTicketsByParcel(parcelId);
        return ResponseEntity.ok(BaseResponse.success(tickets));
    }

    /**
     * Get tickets by delivery assignment ID
     */
    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<BaseResponse<List<TicketResponse>>> getTicketsByAssignment(
            @PathVariable String assignmentId) {
        
        log.debug("Get tickets by assignment: assignmentId={}", assignmentId);
        List<TicketResponse> tickets = ticketService.getTicketsByAssignment(assignmentId);
        return ResponseEntity.ok(BaseResponse.success(tickets));
    }

    /**
     * Get tickets by status (paginated) - Admin only
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<BaseResponse<Page<TicketResponse>>> getTicketsByStatus(
            @PathVariable TicketStatus status,
            @RequestHeader("X-User-Id") String userId,
            Pageable pageable) {
        
        log.debug("Get tickets by status: status={}, userId={}", status, userId);
        Page<TicketResponse> tickets = ticketService.getTicketsByStatus(status, pageable);
        return ResponseEntity.ok(BaseResponse.success(tickets));
    }

    /**
     * Get tickets by type (paginated) - Admin only
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<BaseResponse<Page<TicketResponse>>> getTicketsByType(
            @PathVariable TicketType type,
            @RequestHeader("X-User-Id") String userId,
            Pageable pageable) {
        
        log.debug("Get tickets by type: type={}, userId={}", type, userId);
        Page<TicketResponse> tickets = ticketService.getTicketsByType(type, pageable);
        return ResponseEntity.ok(BaseResponse.success(tickets));
    }

    /**
     * Update ticket status (admin action)
     */
    @PutMapping("/{ticketId}")
    public ResponseEntity<BaseResponse<TicketResponse>> updateTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody UpdateTicketRequest request,
            @RequestHeader("X-User-Id") String adminUserId) {
        
        log.debug("Update ticket: ticketId={}, status={}, adminUserId={}", 
            ticketId, request.getStatus(), adminUserId);
        
        TicketResponse ticket = ticketService.updateTicket(ticketId, request, adminUserId);
        return ResponseEntity.ok(BaseResponse.success(ticket));
    }

    /**
     * Resolve ticket (admin action)
     */
    @PutMapping("/{ticketId}/resolve")
    public ResponseEntity<BaseResponse<TicketResponse>> resolveTicket(
            @PathVariable String ticketId,
            @RequestBody(required = false) String resolutionNotes,
            @RequestHeader("X-User-Id") String adminUserId) {
        
        log.debug("Resolve ticket: ticketId={}, adminUserId={}", ticketId, adminUserId);
        
        TicketResponse ticket = ticketService.resolveTicket(
            ticketId, 
            resolutionNotes != null ? resolutionNotes : "", 
            adminUserId
        );
        return ResponseEntity.ok(BaseResponse.success(ticket));
    }

    /**
     * Cancel ticket (admin action)
     */
    @PutMapping("/{ticketId}/cancel")
    public ResponseEntity<BaseResponse<TicketResponse>> cancelTicket(
            @PathVariable String ticketId,
            @RequestHeader("X-User-Id") String adminUserId) {
        
        log.debug("Cancel ticket: ticketId={}, adminUserId={}", ticketId, adminUserId);
        
        TicketResponse ticket = ticketService.cancelTicket(ticketId, adminUserId);
        return ResponseEntity.ok(BaseResponse.success(ticket));
    }

    /**
     * Reassign parcel (admin action)
     * Note: This only updates the ticket's deliveryAssignmentId
     * Actual parcel reassignment should be handled by session-service
     */
    @PutMapping("/{ticketId}/reassign")
    public ResponseEntity<BaseResponse<TicketResponse>> reassignParcel(
            @PathVariable String ticketId,
            @Valid @RequestBody ReassignParcelRequest request,
            @RequestHeader("X-User-Id") String adminUserId) {
        
        log.debug("Reassign parcel: ticketId={}, newAssignmentId={}, adminUserId={}", 
            ticketId, request.getDeliveryAssignmentId(), adminUserId);
        
        TicketResponse ticket = ticketService.reassignParcel(ticketId, request, adminUserId);
        return ResponseEntity.ok(BaseResponse.success(ticket));
    }

    /**
     * Get ticket statistics - Admin only
     */
    @GetMapping("/statistics")
    public ResponseEntity<BaseResponse<TicketService.TicketStatistics>> getStatistics(
            @RequestHeader("X-User-Id") String userId) {
        
        log.debug("Get ticket statistics: userId={}", userId);
        TicketService.TicketStatistics statistics = ticketService.getStatistics();
        return ResponseEntity.ok(BaseResponse.success(statistics));
    }

    /**
     * Get ticket statistics for current user
     */
    @GetMapping("/statistics/my")
    public ResponseEntity<BaseResponse<TicketService.TicketStatistics>> getMyStatistics(
            @RequestHeader("X-User-Id") String userId) {
        
        log.debug("Get my ticket statistics: userId={}", userId);
        TicketService.TicketStatistics statistics = ticketService.getStatisticsForUser(userId);
        return ResponseEntity.ok(BaseResponse.success(statistics));
    }

    /**
     * Internal API: Create DELIVERY_FAILED ticket (called by session-service)
     * No authentication required - internal service call
     */
    @PostMapping("/delivery-failed")
    public ResponseEntity<BaseResponse<TicketResponse>> createDeliveryFailedTicketInternal(
            @RequestBody CreateDeliveryFailedTicketRequest request) {
        
        log.debug("[communication-service] [TicketController.createDeliveryFailedTicketInternal] Creating DELIVERY_FAILED ticket: parcelId={}, assignmentId={}, shipperId={}", 
            request.parcelId(), request.deliveryAssignmentId(), request.shipperId());
        
        TicketResponse ticket = ticketService.createDeliveryFailedTicket(
            request.parcelId(),
            request.deliveryAssignmentId(),
            request.shipperId(),
            request.description()
        );
        return ResponseEntity.ok(BaseResponse.success(ticket));
    }

    /**
     * Internal API: Create NOT_RECEIVED ticket (called by parcel-service)
     * No authentication required - internal service call
     */
    @PostMapping("/not-received")
    public ResponseEntity<BaseResponse<TicketResponse>> createNotReceivedTicketInternal(
            @RequestBody CreateNotReceivedTicketRequest request) {
        
        log.debug("[communication-service] [TicketController.createNotReceivedTicketInternal] Creating NOT_RECEIVED ticket: parcelId={}, assignmentId={}, clientId={}", 
            request.parcelId(), request.deliveryAssignmentId(), request.clientId());
        
        TicketResponse ticket = ticketService.createNotReceivedTicket(
            request.parcelId(),
            request.deliveryAssignmentId(),
            request.clientId(),
            request.description()
        );
        return ResponseEntity.ok(BaseResponse.success(ticket));
    }

    /**
     * Request DTO for creating delivery failed ticket (internal API)
     */
    public record CreateDeliveryFailedTicketRequest(
        String parcelId,
        String deliveryAssignmentId,
        String shipperId,
        String description
    ) {}

    /**
     * Request DTO for creating not received ticket (internal API)
     */
    public record CreateNotReceivedTicketRequest(
        String parcelId,
        String deliveryAssignmentId,
        String clientId,
        String description
    ) {}
}
