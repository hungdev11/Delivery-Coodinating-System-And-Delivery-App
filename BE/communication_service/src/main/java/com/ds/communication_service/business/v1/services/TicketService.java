package com.ds.communication_service.business.v1.services;

import com.ds.communication_service.app_context.models.Ticket;
import com.ds.communication_service.app_context.repositories.TicketRepository;
import com.ds.communication_service.common.dto.CreateTicketRequest;
import com.ds.communication_service.common.dto.PagedData;
import com.ds.communication_service.common.dto.ReassignParcelRequest;
import com.ds.communication_service.common.dto.TicketResponse;
import com.ds.communication_service.common.dto.UpdateTicketRequest;
import com.ds.communication_service.common.dto.filter.v2.FilterGroupItemV2;
import com.ds.communication_service.common.dto.request.PagingRequestV2;
import com.ds.communication_service.common.dto.sort.SortConfig;
import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;
import com.ds.communication_service.common.utils.EnhancedQueryParserV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing tickets
 * Handles creation, querying, and resolution of delivery issue tickets
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;

    /**
     * Create a new ticket
     * 
     * @param request Ticket creation request
     * @return Created ticket
     */
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setParcelId(request.getParcelId());
        ticket.setDeliveryAssignmentId(request.getDeliveryAssignmentId());
        ticket.setUserId(request.getUserId());
        ticket.setType(request.getType());
        ticket.setStatus(TicketStatus.PENDING);
        ticket.setDescription(request.getDescription());

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("✅ Ticket created: id={}, parcelId={}, type={}, userId={}", 
            savedTicket.getId(), savedTicket.getParcelId(), savedTicket.getType(), savedTicket.getUserId());

        return toResponse(savedTicket);
    }

    /**
     * Helper method to create a DELIVERY_FAILED ticket
     * Can be called from other services when shipper reports delivery failure
     * 
     * @param parcelId Parcel ID
     * @param deliveryAssignmentId Delivery assignment ID (optional)
     * @param shipperId Shipper user ID
     * @param description Description of the failure (optional)
     * @return Created ticket
     */
    @Transactional
    public TicketResponse createDeliveryFailedTicket(String parcelId, String deliveryAssignmentId, String shipperId, String description) {
        CreateTicketRequest request = CreateTicketRequest.builder()
            .parcelId(parcelId)
            .deliveryAssignmentId(deliveryAssignmentId)
            .userId(shipperId)
            .type(TicketType.DELIVERY_FAILED)
            .description(description)
            .build();
        return createTicket(request);
    }

    /**
     * Helper method to create a NOT_RECEIVED ticket
     * Can be called from other services when client reports not receiving parcel
     * 
     * @param parcelId Parcel ID
     * @param deliveryAssignmentId Delivery assignment ID (optional)
     * @param clientId Client user ID
     * @param description Description of the issue (optional)
     * @return Created ticket
     */
    @Transactional
    public TicketResponse createNotReceivedTicket(String parcelId, String deliveryAssignmentId, String clientId, String description) {
        CreateTicketRequest request = CreateTicketRequest.builder()
            .parcelId(parcelId)
            .deliveryAssignmentId(deliveryAssignmentId)
            .userId(clientId)
            .type(TicketType.NOT_RECEIVED)
            .description(description)
            .build();
        return createTicket(request);
    }

    /**
     * Get ticket by ID
     * 
     * @param ticketId Ticket ID
     * @return Ticket
     */
    public TicketResponse getTicket(String ticketId) {
        UUID id = UUID.fromString(ticketId);
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        return toResponse(ticket);
    }

    /**
     * Get all tickets (paginated) - for admin
     * 
     * @param pageable Pagination info
     * @return Page of tickets
     */
    public Page<TicketResponse> getAllTickets(Pageable pageable) {
        return ticketRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map(this::toResponse);
    }

    /**
     * Get tickets by user ID (paginated)
     * 
     * @param userId User ID
     * @param pageable Pagination info
     * @return Page of tickets
     */
    public Page<TicketResponse> getTicketsByUser(String userId, Pageable pageable) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(this::toResponse);
    }

    /**
     * Get tickets by parcel ID
     * 
     * @param parcelId Parcel ID
     * @return List of tickets
     */
    public List<TicketResponse> getTicketsByParcel(String parcelId) {
        return ticketRepository.findByParcelIdOrderByCreatedAtDesc(parcelId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get tickets by delivery assignment ID
     * 
     * @param assignmentId Delivery assignment ID
     * @return List of tickets
     */
    public List<TicketResponse> getTicketsByAssignment(String assignmentId) {
        return ticketRepository.findByDeliveryAssignmentIdOrderByCreatedAtDesc(assignmentId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get tickets by status (paginated)
     * 
     * @param status Ticket status
     * @param pageable Pagination info
     * @return Page of tickets
     */
    public Page<TicketResponse> getTicketsByStatus(TicketStatus status, Pageable pageable) {
        return ticketRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
            .map(this::toResponse);
    }

    /**
     * Get tickets by type (paginated)
     * 
     * @param type Ticket type
     * @param pageable Pagination info
     * @return Page of tickets
     */
    public Page<TicketResponse> getTicketsByType(TicketType type, Pageable pageable) {
        return ticketRepository.findByTypeOrderByCreatedAtDesc(type, pageable)
            .map(this::toResponse);
    }

    /**
     * Get tickets by status and type (paginated)
     * 
     * @param status Ticket status
     * @param type Ticket type
     * @param pageable Pagination info
     * @return Page of tickets
     */
    public Page<TicketResponse> getTicketsByStatusAndType(TicketStatus status, TicketType type, Pageable pageable) {
        return ticketRepository.findByStatusAndTypeOrderByCreatedAtDesc(status, type, pageable)
            .map(this::toResponse);
    }

    /**
     * Update ticket status (admin action)
     * 
     * @param ticketId Ticket ID
     * @param request Update request
     * @param adminUserId Admin user ID who is performing the action
     * @return Updated ticket
     */
    @Transactional
    public TicketResponse updateTicket(String ticketId, UpdateTicketRequest request, String adminUserId) {
        UUID id = UUID.fromString(ticketId);
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus());
            
            // If resolving, set resolvedAt and resolvedBy
            if (request.getStatus() == TicketStatus.RESOLVED) {
                ticket.setResolvedAt(LocalDateTime.now());
                ticket.setResolvedBy(adminUserId);
            }
        }

        if (request.getResolutionNotes() != null) {
            ticket.setResolutionNotes(request.getResolutionNotes());
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("✅ Ticket updated: id={}, status={}, adminUserId={}", 
            savedTicket.getId(), savedTicket.getStatus(), adminUserId);

        return toResponse(savedTicket);
    }

    /**
     * Resolve ticket (admin action)
     * 
     * @param ticketId Ticket ID
     * @param resolutionNotes Resolution notes
     * @param adminUserId Admin user ID
     * @return Resolved ticket
     */
    @Transactional
    public TicketResponse resolveTicket(String ticketId, String resolutionNotes, String adminUserId) {
        UUID id = UUID.fromString(ticketId);
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new IllegalStateException("Ticket is already resolved");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Cannot resolve a cancelled ticket");
        }

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        ticket.setResolvedBy(adminUserId);
        ticket.setResolutionNotes(resolutionNotes);

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("✅ Ticket resolved: id={}, adminUserId={}", savedTicket.getId(), adminUserId);

        return toResponse(savedTicket);
    }

    /**
     * Cancel ticket (admin action)
     * 
     * @param ticketId Ticket ID
     * @param adminUserId Admin user ID
     * @return Cancelled ticket
     */
    @Transactional
    public TicketResponse cancelTicket(String ticketId, String adminUserId) {
        UUID id = UUID.fromString(ticketId);
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new IllegalStateException("Cannot cancel a resolved ticket");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is already cancelled");
        }

        ticket.setStatus(TicketStatus.CANCELLED);

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("✅ Ticket cancelled: id={}, adminUserId={}", savedTicket.getId(), adminUserId);

        return toResponse(savedTicket);
    }

    /**
     * Reassign parcel (admin action)
     * This method updates the deliveryAssignmentId of the ticket
     * Note: Actual parcel reassignment should be handled by session-service
     * 
     * @param ticketId Ticket ID
     * @param request Reassign request
     * @param adminUserId Admin user ID
     * @return Updated ticket
     */
    @Transactional
    public TicketResponse reassignParcel(String ticketId, ReassignParcelRequest request, String adminUserId) {
        UUID id = UUID.fromString(ticketId);
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new IllegalStateException("Cannot reassign parcel for a resolved ticket");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Cannot reassign parcel for a cancelled ticket");
        }

        ticket.setDeliveryAssignmentId(request.getDeliveryAssignmentId());
        if (request.getNotes() != null) {
            ticket.setDescription(
                (ticket.getDescription() != null ? ticket.getDescription() + "\n\n" : "") +
                "Reassignment notes: " + request.getNotes()
            );
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("✅ Parcel reassigned in ticket: id={}, newAssignmentId={}, adminUserId={}", 
            savedTicket.getId(), savedTicket.getDeliveryAssignmentId(), adminUserId);

        return toResponse(savedTicket);
    }

    /**
     * Get ticket statistics
     * 
     * @return Statistics object
     */
    public TicketStatistics getStatistics() {
        long pendingCount = ticketRepository.countByStatus(TicketStatus.PENDING);
        long resolvedCount = ticketRepository.countByStatus(TicketStatus.RESOLVED);
        long cancelledCount = ticketRepository.countByStatus(TicketStatus.CANCELLED);
        long totalCount = ticketRepository.count();

        return new TicketStatistics(pendingCount, resolvedCount, cancelledCount, totalCount);
    }

    /**
     * Get ticket statistics for a user
     * 
     * @param userId User ID
     * @return Statistics object
     */
    public TicketStatistics getStatisticsForUser(String userId) {
        long pendingCount = ticketRepository.countByUserIdAndStatus(userId, TicketStatus.PENDING);
        long resolvedCount = ticketRepository.countByUserIdAndStatus(userId, TicketStatus.RESOLVED);
        long cancelledCount = ticketRepository.countByUserIdAndStatus(userId, TicketStatus.CANCELLED);
        long totalCount = pendingCount + resolvedCount + cancelledCount;

        return new TicketStatistics(pendingCount, resolvedCount, cancelledCount, totalCount);
    }

    /**
     * Get tickets with V2 filter system (POST endpoint)
     * 
     * @param request Paging request with V2 filters
     * @return Paged data response
     */
    @Transactional(readOnly = true)
    public PagedData<TicketResponse> getTicketsV2(PagingRequestV2 request) {
        log.debug("[communication-service] [TicketService.getTicketsV2] Getting tickets with V2 filters: page={}, size={}", 
            request.getPage(), request.getSize());
        
        // Build specification from V2 filters
        Specification<Ticket> spec = Specification.where(null);
        if (request.getFiltersOrNull() != null) {
            log.debug("[communication-service] [TicketService.getTicketsV2] Parsing filters: {}", request.getFiltersOrNull());
            Specification<Ticket> filterSpec = EnhancedQueryParserV2.parseFilterGroup(request.getFiltersOrNull(), Ticket.class);
            if (filterSpec != null) {
                spec = filterSpec;
                log.debug("[communication-service] [TicketService.getTicketsV2] Filter specification created successfully");
            } else {
                log.debug("[communication-service] [TicketService.getTicketsV2] Filter specification is null, using default (no filter)");
            }
        } else {
            log.debug("[communication-service] [TicketService.getTicketsV2] No filters provided");
        }
        
        // Build sort
        Sort sort = buildSort(request.getSortsOrEmpty());
        
        // Build pageable
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        
        // Execute query
        Page<Ticket> page = ticketRepository.findAll(spec, pageable);
        
        // Convert to TicketResponse
        List<TicketResponse> ticketResponses = page.getContent().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        // Build paged data response
        PagedData.Paging<String> paging = PagedData.Paging.<String>builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .filters(request.getFiltersOrNull())
            .sorts(request.getSortsOrEmpty())
            .selected(request.getSelectedOrEmpty())
            .build();
        
        return PagedData.<TicketResponse>builder()
            .data(ticketResponses)
            .page(paging)
            .build();
    }
    
    /**
     * Build Sort from SortConfig list
     */
    private Sort buildSort(List<SortConfig> sortConfigs) {
        if (sortConfigs == null || sortConfigs.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "createdAt"); // Default sort
        }
        
        List<Sort.Order> orders = sortConfigs.stream()
            .map(config -> {
                Sort.Direction direction = "desc".equalsIgnoreCase(config.getDirection()) 
                    ? Sort.Direction.DESC 
                    : Sort.Direction.ASC;
                return new Sort.Order(direction, config.getField());
            })
            .collect(Collectors.toList());
        
        return Sort.by(orders);
    }

    /**
     * Convert Ticket entity to TicketResponse DTO
     */
    private TicketResponse toResponse(Ticket ticket) {
        return TicketResponse.builder()
            .id(ticket.getId().toString())
            .parcelId(ticket.getParcelId())
            .deliveryAssignmentId(ticket.getDeliveryAssignmentId())
            .userId(ticket.getUserId())
            .type(ticket.getType())
            .status(ticket.getStatus())
            .description(ticket.getDescription())
            .createdAt(ticket.getCreatedAt())
            .updatedAt(ticket.getUpdatedAt())
            .resolvedAt(ticket.getResolvedAt())
            .resolvedBy(ticket.getResolvedBy())
            .resolutionNotes(ticket.getResolutionNotes())
            .build();
    }

    /**
     * Statistics class for tickets
     */
    public static class TicketStatistics {
        private final long pendingCount;
        private final long resolvedCount;
        private final long cancelledCount;
        private final long totalCount;

        public TicketStatistics(long pendingCount, long resolvedCount, long cancelledCount, long totalCount) {
            this.pendingCount = pendingCount;
            this.resolvedCount = resolvedCount;
            this.cancelledCount = cancelledCount;
            this.totalCount = totalCount;
        }

        public long getPendingCount() {
            return pendingCount;
        }

        public long getResolvedCount() {
            return resolvedCount;
        }

        public long getCancelledCount() {
            return cancelledCount;
        }

        public long getTotalCount() {
            return totalCount;
        }
    }
}
