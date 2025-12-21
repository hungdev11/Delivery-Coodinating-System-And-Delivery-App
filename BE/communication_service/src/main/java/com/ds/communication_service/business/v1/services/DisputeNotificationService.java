package com.ds.communication_service.business.v1.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ds.communication_service.app_context.models.Conversation;
import com.ds.communication_service.app_context.models.InteractiveProposal;
import com.ds.communication_service.app_context.models.Message;
import com.ds.communication_service.app_context.models.ProposalTypeConfig;
import com.ds.communication_service.app_context.repositories.ConversationRepository;
import com.ds.communication_service.app_context.repositories.InteractiveProposalRepository;
import com.ds.communication_service.app_context.repositories.MessageRepository;
import com.ds.communication_service.app_context.repositories.ProposalTypeConfigRepository;
import com.ds.communication_service.common.dto.BaseResponse;
import com.ds.communication_service.common.dto.MessageResponse;
import com.ds.communication_service.common.dto.NotificationMessage;
import com.ds.communication_service.common.enums.ContentType;
import com.ds.communication_service.common.enums.MessageStatus;
import com.ds.communication_service.common.enums.ProposalActionType;
import com.ds.communication_service.common.enums.ProposalStatus;
import com.ds.communication_service.common.enums.ProposalType;
import com.ds.communication_service.infrastructure.kafka.EventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

/**
 * Service to handle dispute notifications and proposals
 * When a parcel enters DISPUTE status:
 * - Send notification to admin
 * - Create proposal for shipper to appeal with evidence
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeNotificationService {

    private final EventProducer eventProducer;
    private final InteractiveProposalRepository proposalRepo;
    private final ProposalTypeConfigRepository configRepo;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    @Value("${services.user.base-url}")
    private String userServiceUrl;

    /**
     * Handle parcel dispute - send notification to admin and create proposal for shipper
     */
    public void handleParcelDispute(
            String parcelId,
            String parcelCode,
            String receiverId,
            String senderId,
            String deliveryManId,
            LocalDateTime disputedAt) {

        log.debug("[communication-service] [DisputeNotificationService.handleParcelDispute] Handling dispute for parcel {}", parcelId);

        // 1. Send notification to admin
        sendAdminNotification(parcelId, parcelCode, receiverId);

        // 2. Create proposal for shipper if deliveryManId is available
        if (deliveryManId != null && !deliveryManId.isBlank()) {
            createShipperAppealProposal(parcelId, parcelCode, receiverId, deliveryManId);
        } else {
            log.warn("[communication-service] [DisputeNotificationService.handleParcelDispute] No deliveryManId for parcel {}, cannot create shipper proposal", parcelId);
        }
    }

    /**
     * Send notification to admin users
     */
    private void sendAdminNotification(String parcelId, String parcelCode, String receiverId) {
        try {
            // Get admin user IDs from user service
            List<String> adminIds = getAdminUserIds();

            if (adminIds.isEmpty()) {
                log.warn("[communication-service] [DisputeNotificationService.sendAdminNotification] No admin users found");
                return;
            }

            // Send notification to each admin
            for (String adminId : adminIds) {
                NotificationMessage notification = NotificationMessage.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(adminId)
                        .type(NotificationMessage.NotificationType.WARNING)
                        .title("Tranh chấp đơn hàng")
                        .message(String.format("Khách hàng báo chưa nhận được đơn hàng %s", parcelCode))
                        .actionUrl("/parcels?status=DISPUTE") // Link to admin parcels page filtered by DISPUTE
                        .read(false)
                        .createdAt(LocalDateTime.now())
                        .build();

                eventProducer.publishNotification(adminId, notification);
                log.debug("[communication-service] [DisputeNotificationService.sendAdminNotification] Sent notification to admin {}", adminId);
            }
        } catch (Exception e) {
            log.error("[communication-service] [DisputeNotificationService.sendAdminNotification] Failed to send admin notification", e);
        }
    }

    /**
     * Create proposal for shipper to appeal with evidence
     */
    private void createShipperAppealProposal(String parcelId, String parcelCode, String receiverId, String deliveryManId) {
        try {
            // Get or create conversation between receiver and shipper
            Conversation conversation = findOrCreateConversation(receiverId, deliveryManId);

            // Get proposal config for DISPUTE_APPEAL
            ProposalTypeConfig config = configRepo.findByType(ProposalType.DISPUTE_APPEAL)
                    .orElseGet(() -> createDefaultDisputeAppealConfig());

            // Create proposal data
            ObjectNode proposalData = objectMapper.createObjectNode();
            proposalData.put("parcelId", parcelId);
            proposalData.put("parcelCode", parcelCode);
            proposalData.put("reason", "Khách hàng báo chưa nhận được hàng");
            proposalData.put("correlationId", "dispute:" + parcelId); // For cancellation tracking

            // Create proposal
            InteractiveProposal proposal = new InteractiveProposal();
            proposal.setConversation(conversation);
            proposal.setProposerId(receiverId); // Client creates the "dispute"
            proposal.setRecipientId(deliveryManId); // Shipper receives appeal request
            proposal.setType(ProposalType.DISPUTE_APPEAL);
            proposal.setData(proposalData.toString());
            proposal.setStatus(ProposalStatus.PENDING);
            proposal.setActionType(config.getResponseActionType());
            // Store correlation ID in data field for cancellation tracking
            // sessionId is UUID type, so we can't use it for string correlation

            if (config.getDefaultTimeoutMinutes() != null) {
                proposal.setExpiresAt(LocalDateTime.now().plusMinutes(config.getDefaultTimeoutMinutes()));
            }

            InteractiveProposal savedProposal = proposalRepo.save(proposal);

            // Create message for proposal
            Message message = new Message();
            message.setConversation(conversation);
            message.setSenderId(receiverId);
            message.setType(ContentType.INTERACTIVE_PROPOSAL);
            message.setContent(String.format("Đơn hàng %s - Khách hàng báo chưa nhận được hàng. Bạn có thể kháng cáo và gửi bằng chứng.", parcelCode));
            message.setStatus(MessageStatus.SENT);
            message.setSentAt(LocalDateTime.now());
            message.setProposal(savedProposal);

            Message savedMessage = messageRepo.save(message);

            // Send via WebSocket
            MessageResponse messageResponse = toDto(savedMessage);
            
            // Send to shipper (recipient of proposal)
            messagingTemplate.convertAndSendToUser(deliveryManId, "/queue/messages", messageResponse);
            log.debug("[communication-service] [DisputeNotificationService.createShipperAppealProposal] Sent DISPUTE_APPEAL message to shipper {} via WebSocket", deliveryManId);
            
            // Send to client (proposer) - they should see the message in chat
            messagingTemplate.convertAndSendToUser(receiverId, "/queue/messages", messageResponse);
            log.info("[communication-service] [DisputeNotificationService.createShipperAppealProposal] Sent DISPUTE_APPEAL message to client {} via WebSocket. MessageId: {}, ConversationId: {}", 
                    receiverId, savedMessage.getId(), conversation.getId());

            log.debug("[communication-service] [DisputeNotificationService.createShipperAppealProposal] Created proposal {} for shipper {}", savedProposal.getId(), deliveryManId);

        } catch (Exception e) {
            log.error("[communication-service] [DisputeNotificationService.createShipperAppealProposal] Failed to create shipper proposal", e);
        }
    }

    /**
     * Cancel dispute-related proposals when client retracts
     */
    public void cancelDisputeProposals(String parcelId) {
        try {
            String correlationId = "dispute:" + parcelId;
            // Find proposals by searching in data field for correlation ID
            List<InteractiveProposal> proposals = proposalRepo.findByDataContainingAndStatus(correlationId, ProposalStatus.PENDING);

            for (InteractiveProposal proposal : proposals) {
                proposal.setStatus(ProposalStatus.CANCELLED);
                proposalRepo.save(proposal);

                // Send WebSocket update
                com.ds.communication_service.common.dto.ProposalUpdateRequest updateDto = 
                        new com.ds.communication_service.common.dto.ProposalUpdateRequest(
                            proposal.getId(),
                            ProposalStatus.CANCELLED,
                            proposal.getConversation().getId(),
                            "Khách hàng đã xác nhận nhận được hàng"
                        );

                messagingTemplate.convertAndSendToUser(
                    proposal.getRecipientId(), 
                    "/queue/proposal-updates", 
                    updateDto
                );
                messagingTemplate.convertAndSendToUser(
                    proposal.getProposerId(), 
                    "/queue/proposal-updates", 
                    updateDto
                );

                log.debug("[communication-service] [DisputeNotificationService.cancelDisputeProposals] Cancelled proposal {} for parcel {}", proposal.getId(), parcelId);
            }
        } catch (Exception e) {
            log.error("[communication-service] [DisputeNotificationService.cancelDisputeProposals] Failed to cancel proposals for parcel {}", parcelId, e);
        }
    }

    /**
     * Get admin user IDs from user service
     */
    @SuppressWarnings("unchecked")
    private List<String> getAdminUserIds() {
        try {
            String url = userServiceUrl + "/api/v1/users/by-role?role=ADMIN";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<BaseResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    BaseResponse.class
            );

            if (response.getBody() != null && response.getBody().getResult() != null) {
                Object result = response.getBody().getResult();
                if (result instanceof List) {
                    return (List<String>) result;
                }
            }
            return List.of();
        } catch (Exception e) {
            log.error("[communication-service] [DisputeNotificationService.getAdminUserIds] Failed to fetch admin users", e);
            // Fallback: use environment variable if available
            String fallbackAdmin = System.getenv("ADMIN_USER_ID");
            if (fallbackAdmin != null && !fallbackAdmin.isBlank()) {
                return List.of(fallbackAdmin);
            }
            return List.of();
        }
    }

    /**
     * Find or create conversation between two users
     * Note: Conversation model enforces user1Id < user2Id constraint
     */
    private Conversation findOrCreateConversation(String user1Id, String user2Id) {
        // Ensure user1Id < user2Id for constraint compliance
        String u1 = user1Id.compareTo(user2Id) < 0 ? user1Id : user2Id;
        String u2 = user1Id.compareTo(user2Id) < 0 ? user2Id : user1Id;
        
        return conversationRepo.findByUser1IdAndUser2Id(u1, u2)
                .orElseGet(() -> {
                    Conversation newConv = new Conversation();
                    newConv.setUser1Id(u1);
                    newConv.setUser2Id(u2);
                    // createdAt is auto-generated by @CreationTimestamp
                    return conversationRepo.save(newConv);
                });
    }

    /**
     * Create default dispute appeal config if not exists
     */
    private ProposalTypeConfig createDefaultDisputeAppealConfig() {
        ProposalTypeConfig config = new ProposalTypeConfig();
        config.setType(ProposalType.DISPUTE_APPEAL);
        config.setRequiredRole("SHIPPER");
        config.setCreationActionType(ProposalActionType.TEXT_INPUT);
        config.setResponseActionType(ProposalActionType.TEXT_INPUT);
        config.setDescription("Appeal for disputed parcel delivery");
        config.setDefaultTimeoutMinutes(2880L); // 48 hours (Long type)
        return configRepo.save(config);
    }

    /**
     * Convert Message to DTO
     */
    private MessageResponse toDto(Message message) {
        MessageResponse dto = new MessageResponse();
        dto.setId(message.getId().toString());
        dto.setConversationId(message.getConversation().getId().toString());
        dto.setSenderId(message.getSenderId());
        dto.setType(message.getType());
        dto.setContent(message.getContent());
        dto.setStatus(message.getStatus());
        dto.setSentAt(message.getSentAt());
        dto.setDeliveredAt(message.getDeliveredAt());
        dto.setReadAt(message.getReadAt());

        if (message.getProposal() != null) {
            dto.setProposal(com.ds.communication_service.common.dto.InteractiveProposalResponseDTO.from(message.getProposal()));
        }

        return dto;
    }
}
