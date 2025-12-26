package com.ds.communication_service.business.v1.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.communication_service.app_context.models.Ticket;
import com.ds.communication_service.app_context.repositories.TicketRepository;
import com.ds.communication_service.common.dto.CreateTicketRequest;
import com.ds.communication_service.common.dto.TicketResponse;
import com.ds.communication_service.common.dto.UpdateTicketRequest;
import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;
import com.ds.communication_service.common.interfaces.ITicketService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing tickets
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketService implements ITicketService {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, String reporterId) {
        log.info("Creating ticket: type={}, parcelId={}, reporterId={}", 
                request.getType(), request.getParcelId(), reporterId);

        Ticket ticket = new Ticket();
        ticket.setType(request.getType());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setParcelId(request.getParcelId());
        ticket.setAssignmentId(request.getAssignmentId());
        ticket.setReporterId(reporterId);
        ticket.setDescription(request.getDescription());

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket created: id={}", saved.getId());

        return toDto(saved);
    }

    @Override
    public TicketResponse getTicketById(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> {
                    log.warn("Ticket not found: id={}", ticketId);
                    return new EntityNotFoundException("Ticket not found with ID: " + ticketId);
                });

        return toDto(ticket);
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(UUID ticketId, UpdateTicketRequest request, String adminId) {
        log.info("Updating ticket: id={}, status={}, adminId={}", 
                ticketId, request.getStatus(), adminId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> {
                    log.warn("Ticket not found: id={}", ticketId);
                    return new EntityNotFoundException("Ticket not found with ID: " + ticketId);
                });

        // Update status
        ticket.setStatus(request.getStatus());
        
        // Assign admin if not already assigned
        if (ticket.getAssignedAdminId() == null) {
            ticket.setAssignedAdminId(adminId);
        }

        // Update resolution notes if provided
        if (request.getResolutionNotes() != null && !request.getResolutionNotes().isBlank()) {
            ticket.setResolutionNotes(request.getResolutionNotes());
        }

        // Update action taken if provided
        if (request.getActionTaken() != null && !request.getActionTaken().isBlank()) {
            ticket.setActionTaken(request.getActionTaken());
        }

        // Set resolved timestamp if status is RESOLVED
        if (request.getStatus() == TicketStatus.RESOLVED && ticket.getResolvedAt() == null) {
            ticket.setResolvedAt(LocalDateTime.now());
        }

        // Update assignment if reassigning
        if (request.getNewAssignmentId() != null && !request.getNewAssignmentId().isBlank()) {
            ticket.setAssignmentId(request.getNewAssignmentId());
            ticket.setActionTaken("REASSIGN");
        }

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket updated: id={}, status={}", saved.getId(), saved.getStatus());

        return toDto(saved);
    }

    @Override
    public Page<TicketResponse> getTicketsByStatus(TicketStatus status, Pageable pageable) {
        return ticketRepository.findByStatus(status, pageable)
                .map(this::toDto);
    }

    @Override
    public Page<TicketResponse> getTicketsByType(TicketType type, Pageable pageable) {
        return ticketRepository.findByType(type, pageable)
                .map(this::toDto);
    }

    @Override
    public Page<TicketResponse> getOpenTickets(Pageable pageable) {
        return ticketRepository.findOpenTickets(pageable)
                .map(this::toDto);
    }

    @Override
    public Page<TicketResponse> getTicketsByReporter(String reporterId, Pageable pageable) {
        return ticketRepository.findByReporterId(reporterId, pageable)
                .map(this::toDto);
    }

    @Override
    public Page<TicketResponse> getTicketsByAssignedAdmin(String adminId, Pageable pageable) {
        return ticketRepository.findByAssignedAdminId(adminId, pageable)
                .map(this::toDto);
    }

    @Override
    public List<TicketResponse> getTicketsByParcelId(String parcelId) {
        return ticketRepository.findByParcelId(parcelId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TicketResponse> getTicketsByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        log.debug("Getting tickets by IDs: count={}", ids.size());
        return ticketRepository.findByIdIn(ids)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TicketResponse> getTicketsByParcelIds(List<String> parcelIds) {
        if (parcelIds == null || parcelIds.isEmpty()) {
            return List.of();
        }
        log.debug("Getting tickets by parcel IDs: count={}", parcelIds.size());
        return ticketRepository.findByParcelIdIn(parcelIds)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TicketResponse> getTicketsByAssignmentIds(List<String> assignmentIds) {
        if (assignmentIds == null || assignmentIds.isEmpty()) {
            return List.of();
        }
        log.debug("Getting tickets by assignment IDs: count={}", assignmentIds.size());
        return ticketRepository.findByAssignmentIdIn(assignmentIds)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TicketResponse> getTicketsByReporterIds(List<String> reporterIds) {
        if (reporterIds == null || reporterIds.isEmpty()) {
            return List.of();
        }
        log.debug("Getting tickets by reporter IDs: count={}", reporterIds.size());
        return ticketRepository.findByReporterIdIn(reporterIds)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<TicketResponse> getTicketsByStatusAndType(TicketStatus status, TicketType type, Pageable pageable) {
        return ticketRepository.findByStatusAndType(status, type, pageable)
                .map(this::toDto);
    }

    @Override
    public Page<TicketResponse> getTicketsByIdsPaged(List<UUID> ids, Pageable pageable) {
        if (ids == null || ids.isEmpty()) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        log.debug("Getting tickets by IDs with paging: count={}, page={}, size={}", ids.size(), pageable.getPageNumber(), pageable.getPageSize());
        
        List<Ticket> allTickets = ticketRepository.findByIdIn(ids);
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allTickets.size());
        List<Ticket> pagedTickets = start < allTickets.size() ? allTickets.subList(start, end) : List.of();
        
        List<TicketResponse> content = pagedTickets.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(content, pageable, allTickets.size());
    }

    @Override
    public Page<TicketResponse> getTicketsByParcelIdsPaged(List<String> parcelIds, Pageable pageable) {
        if (parcelIds == null || parcelIds.isEmpty()) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        log.debug("Getting tickets by parcel IDs with paging: count={}, page={}, size={}", parcelIds.size(), pageable.getPageNumber(), pageable.getPageSize());
        
        List<Ticket> allTickets = ticketRepository.findByParcelIdIn(parcelIds);
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allTickets.size());
        List<Ticket> pagedTickets = start < allTickets.size() ? allTickets.subList(start, end) : List.of();
        
        List<TicketResponse> content = pagedTickets.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(content, pageable, allTickets.size());
    }

    @Override
    public Page<TicketResponse> getTicketsByAssignmentIdsPaged(List<String> assignmentIds, Pageable pageable) {
        if (assignmentIds == null || assignmentIds.isEmpty()) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        log.debug("Getting tickets by assignment IDs with paging: count={}, page={}, size={}", assignmentIds.size(), pageable.getPageNumber(), pageable.getPageSize());
        
        List<Ticket> allTickets = ticketRepository.findByAssignmentIdIn(assignmentIds);
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allTickets.size());
        List<Ticket> pagedTickets = start < allTickets.size() ? allTickets.subList(start, end) : List.of();
        
        List<TicketResponse> content = pagedTickets.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(content, pageable, allTickets.size());
    }

    @Override
    public Page<TicketResponse> getTicketsByReporterIdsPaged(List<String> reporterIds, Pageable pageable) {
        if (reporterIds == null || reporterIds.isEmpty()) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        log.debug("Getting tickets by reporter IDs with paging: count={}, page={}, size={}", reporterIds.size(), pageable.getPageNumber(), pageable.getPageSize());
        
        List<Ticket> allTickets = ticketRepository.findByReporterIdIn(reporterIds);
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allTickets.size());
        List<Ticket> pagedTickets = start < allTickets.size() ? allTickets.subList(start, end) : List.of();
        
        List<TicketResponse> content = pagedTickets.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(content, pageable, allTickets.size());
    }

    /**
     * Convert Ticket entity to DTO
     */
    public TicketResponse toDto(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .type(ticket.getType())
                .status(ticket.getStatus())
                .parcelId(ticket.getParcelId())
                .assignmentId(ticket.getAssignmentId())
                .reporterId(ticket.getReporterId())
                .assignedAdminId(ticket.getAssignedAdminId())
                .description(ticket.getDescription())
                .resolutionNotes(ticket.getResolutionNotes())
                .actionTaken(ticket.getActionTaken())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .build();
    }
}
