package com.ds.communication_service.application.controllers.v1;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ds.communication_service.common.dto.BaseResponse;
import com.ds.communication_service.common.dto.CreateTicketRequest;
import com.ds.communication_service.common.dto.PageResponse;
import com.ds.communication_service.common.dto.TicketResponse;
import com.ds.communication_service.common.dto.UpdateTicketRequest;
import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;
import com.ds.communication_service.common.interfaces.ITicketService;
import com.ds.communication_service.app_context.repositories.TicketRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for Ticket management
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final ITicketService ticketService;
    private final TicketRepository ticketRepository;

    /**
     * Create a new ticket
     * Used by shippers (DELIVERY_FAILED) or clients (NOT_RECEIVED)
     */
    @PostMapping
    public ResponseEntity<BaseResponse<TicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @RequestParam String reporterId) {
        log.info("Creating ticket: type={}, parcelId={}, reporterId={}", 
                request.getType(), request.getParcelId(), reporterId);

        TicketResponse ticket = ticketService.createTicket(request, reporterId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(ticket, "Ticket created successfully"));
    }

    /**
     * Get ticket by ID
     */
    @GetMapping("/{ticketId}")
    public ResponseEntity<BaseResponse<TicketResponse>> getTicketById(
            @PathVariable UUID ticketId) {
        log.debug("Getting ticket: id={}", ticketId);

        TicketResponse ticket = ticketService.getTicketById(ticketId);
        return ResponseEntity.ok(BaseResponse.success(ticket));
    }

    /**
     * Update ticket (admin actions: reassign, cancel, resolve)
     */
    @PutMapping("/{ticketId}")
    public ResponseEntity<BaseResponse<TicketResponse>> updateTicket(
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateTicketRequest request,
            @RequestParam String adminId) {
        log.info("Updating ticket: id={}, status={}, adminId={}", 
                ticketId, request.getStatus(), adminId);

        TicketResponse ticket = ticketService.updateTicket(ticketId, request, adminId);
        return ResponseEntity.ok(BaseResponse.success(ticket, "Ticket updated successfully"));
    }

    /**
     * Get tickets with filters
     * Supports filtering by status and/or type
     */
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<TicketResponse>>> getTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("Getting tickets: status={}, type={}", status, type);

        Page<TicketResponse> tickets;
        if (status != null && type != null) {
            // Filter by both status and type
            tickets = ticketService.getTicketsByStatusAndType(status, type, pageable);
        } else if (status != null) {
            tickets = ticketService.getTicketsByStatus(status, pageable);
        } else if (type != null) {
            tickets = ticketService.getTicketsByType(type, pageable);
        } else {
            // Default: return open tickets
            tickets = ticketService.getOpenTickets(pageable);
        }

        PageResponse<TicketResponse> pageResponse = PageResponse.from(tickets);

        return ResponseEntity.ok(BaseResponse.success(pageResponse));
    }

    /**
     * Get open tickets (not resolved or cancelled)
     */
    @GetMapping("/open")
    public ResponseEntity<BaseResponse<PageResponse<TicketResponse>>> getOpenTickets(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("Getting open tickets");

        Page<TicketResponse> tickets = ticketService.getOpenTickets(pageable);
        PageResponse<TicketResponse> pageResponse = PageResponse.from(tickets);

        return ResponseEntity.ok(BaseResponse.success(pageResponse));
    }

    /**
     * Get tickets by reporter ID
     */
    @GetMapping("/reporter/{reporterId}")
    public ResponseEntity<BaseResponse<PageResponse<TicketResponse>>> getTicketsByReporter(
            @PathVariable String reporterId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("Getting tickets by reporter: reporterId={}", reporterId);

        Page<TicketResponse> tickets = ticketService.getTicketsByReporter(reporterId, pageable);
        PageResponse<TicketResponse> pageResponse = PageResponse.from(tickets);

        return ResponseEntity.ok(BaseResponse.success(pageResponse));
    }

    /**
     * Get tickets by assigned admin ID
     */
    @GetMapping("/admin/{adminId}")
    public ResponseEntity<BaseResponse<PageResponse<TicketResponse>>> getTicketsByAssignedAdmin(
            @PathVariable String adminId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("Getting tickets by assigned admin: adminId={}", adminId);

        Page<TicketResponse> tickets = ticketService.getTicketsByAssignedAdmin(adminId, pageable);
        PageResponse<TicketResponse> pageResponse = PageResponse.from(tickets);

        return ResponseEntity.ok(BaseResponse.success(pageResponse));
    }

    /**
     * Get tickets by parcel ID
     */
    @GetMapping("/parcel/{parcelId}")
    public ResponseEntity<BaseResponse<java.util.List<TicketResponse>>> getTicketsByParcelId(
            @PathVariable String parcelId) {
        log.debug("Getting tickets by parcel: parcelId={}", parcelId);

        java.util.List<TicketResponse> tickets = ticketService.getTicketsByParcelId(parcelId);
        return ResponseEntity.ok(BaseResponse.success(tickets));
    }

    /**
     * Bulk query: Get tickets by list of IDs
     * Supports querying multiple tickets at once
     */
    @PostMapping("/bulk/ids")
    public ResponseEntity<BaseResponse<java.util.List<TicketResponse>>> getTicketsByIds(
            @Valid @RequestBody com.ds.communication_service.common.dto.BulkQueryRequest request) {
        log.debug("Getting tickets by IDs: count={}", request.getIds().size());

        java.util.List<UUID> uuids = request.getIds().stream()
                .map(UUID::fromString)
                .collect(java.util.stream.Collectors.toList());
        
        java.util.List<TicketResponse> tickets = ticketService.getTicketsByIds(uuids);
        return ResponseEntity.ok(BaseResponse.success(tickets));
    }

    /**
     * Bulk query: Get tickets by list of parcel IDs
     * Useful for querying tickets for multiple parcels at once
     */
    @PostMapping("/bulk/parcels")
    public ResponseEntity<BaseResponse<java.util.List<TicketResponse>>> getTicketsByParcelIds(
            @Valid @RequestBody com.ds.communication_service.common.dto.BulkQueryRequest request) {
        log.debug("Getting tickets by parcel IDs: count={}", request.getIds().size());

        java.util.List<TicketResponse> tickets = ticketService.getTicketsByParcelIds(request.getIds());
        return ResponseEntity.ok(BaseResponse.success(tickets));
    }

    /**
     * Bulk query: Get tickets by list of assignment IDs
     * Useful for querying tickets for multiple assignments at once
     */
    @PostMapping("/bulk/assignments")
    public ResponseEntity<BaseResponse<java.util.List<TicketResponse>>> getTicketsByAssignmentIds(
            @Valid @RequestBody com.ds.communication_service.common.dto.BulkQueryRequest request) {
        log.debug("Getting tickets by assignment IDs: count={}", request.getIds().size());

        java.util.List<TicketResponse> tickets = ticketService.getTicketsByAssignmentIds(request.getIds());
        return ResponseEntity.ok(BaseResponse.success(tickets));
    }

    /**
     * Bulk query: Get tickets by list of reporter IDs
     * Useful for querying tickets from multiple reporters at once
     */
    @PostMapping("/bulk/reporters")
    public ResponseEntity<BaseResponse<java.util.List<TicketResponse>>> getTicketsByReporterIds(
            @Valid @RequestBody com.ds.communication_service.common.dto.BulkQueryRequest request) {
        log.debug("Getting tickets by reporter IDs: count={}", request.getIds().size());

        java.util.List<TicketResponse> tickets = ticketService.getTicketsByReporterIds(request.getIds());
        return ResponseEntity.ok(BaseResponse.success(tickets));
    }
}
