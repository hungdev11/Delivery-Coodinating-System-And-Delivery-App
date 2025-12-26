package com.ds.communication_service.common.interfaces;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ds.communication_service.common.dto.CreateTicketRequest;
import com.ds.communication_service.common.dto.TicketResponse;
import com.ds.communication_service.common.dto.UpdateTicketRequest;
import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;

/**
 * Interface for Ticket Service
 */
public interface ITicketService {
    
    /**
     * Create a new ticket
     */
    TicketResponse createTicket(CreateTicketRequest request, String reporterId);
    
    /**
     * Get ticket by ID
     */
    TicketResponse getTicketById(UUID ticketId);
    
    /**
     * Update ticket (admin actions)
     */
    TicketResponse updateTicket(UUID ticketId, UpdateTicketRequest request, String adminId);
    
    /**
     * Get tickets by status
     */
    Page<TicketResponse> getTicketsByStatus(TicketStatus status, Pageable pageable);
    
    /**
     * Get tickets by type
     */
    Page<TicketResponse> getTicketsByType(TicketType type, Pageable pageable);
    
    /**
     * Get open tickets (not resolved or cancelled)
     */
    Page<TicketResponse> getOpenTickets(Pageable pageable);
    
    /**
     * Get tickets by reporter ID
     */
    Page<TicketResponse> getTicketsByReporter(String reporterId, Pageable pageable);
    
    /**
     * Get tickets by assigned admin ID
     */
    Page<TicketResponse> getTicketsByAssignedAdmin(String adminId, Pageable pageable);
    
    /**
     * Get tickets by parcel ID
     */
    java.util.List<TicketResponse> getTicketsByParcelId(String parcelId);
    
    /**
     * Get tickets by list of IDs (bulk query)
     */
    java.util.List<TicketResponse> getTicketsByIds(java.util.List<UUID> ids);
    
    /**
     * Get tickets by list of parcel IDs (bulk query)
     */
    java.util.List<TicketResponse> getTicketsByParcelIds(java.util.List<String> parcelIds);
    
    /**
     * Get tickets by list of assignment IDs (bulk query)
     */
    java.util.List<TicketResponse> getTicketsByAssignmentIds(java.util.List<String> assignmentIds);
    
    /**
     * Get tickets by list of reporter IDs (bulk query)
     */
    java.util.List<TicketResponse> getTicketsByReporterIds(java.util.List<String> reporterIds);
    
    /**
     * Get tickets by status and type (combined filter)
     */
    Page<TicketResponse> getTicketsByStatusAndType(TicketStatus status, TicketType type, Pageable pageable);
    
    /**
     * Bulk query with paging: Get tickets by list of IDs (with pagination)
     */
    Page<TicketResponse> getTicketsByIdsPaged(java.util.List<UUID> ids, Pageable pageable);
    
    /**
     * Bulk query with paging: Get tickets by list of parcel IDs (with pagination)
     */
    Page<TicketResponse> getTicketsByParcelIdsPaged(java.util.List<String> parcelIds, Pageable pageable);
    
    /**
     * Bulk query with paging: Get tickets by list of assignment IDs (with pagination)
     */
    Page<TicketResponse> getTicketsByAssignmentIdsPaged(java.util.List<String> assignmentIds, Pageable pageable);
    
    /**
     * Bulk query with paging: Get tickets by list of reporter IDs (with pagination)
     */
    Page<TicketResponse> getTicketsByReporterIdsPaged(java.util.List<String> reporterIds, Pageable pageable);
}
