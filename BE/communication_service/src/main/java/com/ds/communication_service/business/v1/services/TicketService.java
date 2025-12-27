package com.ds.communication_service.business.v1.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.communication_service.app_context.models.Ticket;
import com.ds.communication_service.app_context.repositories.TicketRepository;
import com.ds.communication_service.common.dto.CreateTicketRequest;
import com.ds.communication_service.common.dto.TicketResponse;
import com.ds.communication_service.common.dto.UpdateNotificationDTO;
import com.ds.communication_service.common.dto.UpdateTicketRequest;
import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;
import com.ds.communication_service.common.interfaces.ITicketService;
import com.ds.communication_service.infrastructure.kafka.KafkaConfig;
import com.ds.communication_service.app_context.models.Conversation;
import com.ds.communication_service.app_context.models.InteractiveProposal;
import com.ds.communication_service.app_context.models.Message;
import com.ds.communication_service.app_context.repositories.InteractiveProposalRepository;
import com.ds.communication_service.app_context.repositories.MessageRepository;
import com.ds.communication_service.common.dto.InteractiveProposalResponseDTO;
import com.ds.communication_service.common.dto.MessageResponse;
import com.ds.communication_service.common.enums.ContentType;
import com.ds.communication_service.common.enums.MessageStatus;
import com.ds.communication_service.common.enums.ProposalActionType;
import com.ds.communication_service.common.enums.ProposalStatus;
import com.ds.communication_service.common.enums.ProposalType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ConversationService conversationService;
    private final InteractiveProposalRepository proposalRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final org.springframework.messaging.simp.SimpMessageSendingOperations messagingTemplate;

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

        // Publish update notification for ticket creation
        publishTicketUpdateNotification(saved, UpdateNotificationDTO.ActionType.CREATED);

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
        
        // Track if admin was just assigned (for creating proposal)
        boolean adminJustAssigned = ticket.getAssignedAdminId() == null;
        
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

        // Create proposal if admin was just assigned and ticket is OPEN/IN_PROGRESS
        if (adminJustAssigned && (saved.getStatus() == TicketStatus.OPEN || saved.getStatus() == TicketStatus.IN_PROGRESS)) {
            createTicketProposal(saved);
        }

        // Publish update notification for ticket update
        publishTicketUpdateNotification(saved, UpdateNotificationDTO.ActionType.STATUS_CHANGED);

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

    /**
     * Publish ticket update notification to Kafka
     * Notifies reporter, assigned admin (if any), and all admins for OPEN tickets
     */
    private void publishTicketUpdateNotification(Ticket ticket, UpdateNotificationDTO.ActionType action) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("ticketId", ticket.getId().toString());
            data.put("ticketType", ticket.getType().toString());
            data.put("ticketStatus", ticket.getStatus().toString());
            data.put("parcelId", ticket.getParcelId());
            data.put("assignmentId", ticket.getAssignmentId());
            data.put("reporterId", ticket.getReporterId());
            data.put("assignedAdminId", ticket.getAssignedAdminId());
            data.put("description", ticket.getDescription());

            // Create UpdateNotificationDTO as Map (to match parcel-service pattern)
            Map<String, Object> updateNotification = new HashMap<>();
            updateNotification.put("id", UUID.randomUUID().toString());
            
            // Determine userIds to notify:
            // - Always notify reporter
            // - Notify assigned admin if exists
            // - For OPEN tickets, also notify all admins (they can see in ticket list)
            String userIds = ticket.getReporterId();
            if (ticket.getAssignedAdminId() != null && !ticket.getAssignedAdminId().isBlank()) {
                userIds += "," + ticket.getAssignedAdminId();
            }
            // Note: For OPEN tickets without assigned admin, admins should check ticket list
            // We don't broadcast to all admins here to avoid spam
            
            updateNotification.put("userId", userIds);
            updateNotification.put("updateType", UpdateNotificationDTO.UpdateType.DELIVERY_UPDATE.toString());
            updateNotification.put("entityType", UpdateNotificationDTO.EntityType.ASSIGNMENT.toString()); // Ticket is related to assignment
            updateNotification.put("entityId", ticket.getAssignmentId() != null ? ticket.getAssignmentId() : ticket.getParcelId());
            updateNotification.put("action", action.toString());
            updateNotification.put("data", data);
            updateNotification.put("timestamp", LocalDateTime.now().toString());
            updateNotification.put("message", String.format("Ticket %s: %s - %s", 
                    ticket.getType().toString(), 
                    ticket.getStatus().toString(),
                    ticket.getDescription() != null && ticket.getDescription().length() > 50 
                            ? ticket.getDescription().substring(0, 50) + "..." 
                            : ticket.getDescription()));
            updateNotification.put("clientType", UpdateNotificationDTO.ClientType.ALL.toString());

            // Publish to update-notifications topic
            kafkaTemplate.send(KafkaConfig.TOPIC_UPDATE_NOTIFICATIONS, ticket.getId().toString(), updateNotification);

            log.debug("[communication-service] [TicketService.publishTicketUpdateNotification] Published ticket {} update notification: action={}, userIds={}", 
                    ticket.getId(), action, userIds);
        } catch (Exception e) {
            log.error("[communication-service] [TicketService.publishTicketUpdateNotification] Failed to publish update notification for ticket {}", 
                    ticket.getId(), e);
            // Don't throw - notification failure shouldn't break the transaction
        }
    }

    /**
     * Create InteractiveProposal for ticket in conversation between reporter and assigned admin
     * Allows admin to interact with ticket (ACCEPT, RESOLVE, REJECT) via chat
     */
    private void createTicketProposal(Ticket ticket) {
        try {
            if (ticket.getAssignedAdminId() == null || ticket.getAssignedAdminId().isBlank()) {
                log.debug("[communication-service] [TicketService.createTicketProposal] Ticket {} has no assigned admin, skipping proposal creation", ticket.getId());
                return;
            }

            if (ticket.getReporterId() == null || ticket.getReporterId().isBlank()) {
                log.debug("[communication-service] [TicketService.createTicketProposal] Ticket {} has no reporter, skipping proposal creation", ticket.getId());
                return;
            }

            // Find or create conversation between reporter and admin
            Conversation conversation = conversationService.findOrCreateConversation(
                    ticket.getReporterId(), 
                    ticket.getAssignedAdminId()
            );

            // Create proposal data
            ObjectNode proposalData = objectMapper.createObjectNode();
            proposalData.put("ticketId", ticket.getId().toString());
            proposalData.put("ticketType", ticket.getType().toString());
            proposalData.put("parcelId", ticket.getParcelId());
            proposalData.put("assignmentId", ticket.getAssignmentId());
            proposalData.put("description", ticket.getDescription() != null ? ticket.getDescription() : "");

            // Create proposal
            InteractiveProposal proposal = new InteractiveProposal();
            proposal.setConversation(conversation);
            proposal.setProposerId(ticket.getReporterId()); // Reporter created the ticket
            proposal.setRecipientId(ticket.getAssignedAdminId()); // Admin receives the ticket
            proposal.setType(ProposalType.TICKET);
            proposal.setData(proposalData.toString());
            proposal.setStatus(ProposalStatus.PENDING);
            proposal.setActionType(ProposalActionType.ACCEPT_REJECT); // Admin can ACCEPT (assign to self) or REJECT (cancel)
            // No expiration for tickets - they remain until resolved

            InteractiveProposal savedProposal = proposalRepository.save(proposal);

            // Create message for proposal
            Message message = new Message();
            message.setConversation(conversation);
            message.setSenderId(ticket.getReporterId());
            message.setType(ContentType.INTERACTIVE_PROPOSAL);
            message.setContent(String.format("Ticket %s: %s - %s", 
                    ticket.getType().toString(),
                    ticket.getDescription() != null && ticket.getDescription().length() > 100 
                            ? ticket.getDescription().substring(0, 100) + "..." 
                            : ticket.getDescription()));
            message.setStatus(MessageStatus.SENT);
            message.setSentAt(LocalDateTime.now());
            message.setProposal(savedProposal);

            Message savedMessage = messageRepository.save(message);

            // Convert proposal to DTO using static from() method
            InteractiveProposalResponseDTO proposalDto = InteractiveProposalResponseDTO.from(savedProposal);

            // Convert to DTO and send via WebSocket
            MessageResponse messageResponse = MessageResponse.builder()
                    .id(savedMessage.getId().toString())
                    .conversationId(conversation.getId().toString())
                    .senderId(savedMessage.getSenderId())
                    .content(savedMessage.getContent())
                    .type(savedMessage.getType())
                    .sentAt(savedMessage.getSentAt())
                    .status(savedMessage.getStatus())
                    .proposal(proposalDto)
                    .build();

            // Send to admin (recipient)
            messagingTemplate.convertAndSendToUser(ticket.getAssignedAdminId(), "/queue/messages", messageResponse);
            
            // Send to reporter (proposer) - they should see the message in chat
            messagingTemplate.convertAndSendToUser(ticket.getReporterId(), "/queue/messages", messageResponse);

            log.debug("[communication-service] [TicketService.createTicketProposal] Created ticket proposal {} for ticket {}", 
                    savedProposal.getId(), ticket.getId());
        } catch (Exception e) {
            log.error("[communication-service] [TicketService.createTicketProposal] Failed to create ticket proposal for ticket {}", 
                    ticket.getId(), e);
            // Don't throw - proposal creation failure shouldn't break ticket update
        }
    }
}
