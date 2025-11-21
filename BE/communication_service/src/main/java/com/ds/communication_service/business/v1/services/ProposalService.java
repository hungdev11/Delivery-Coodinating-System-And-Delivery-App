package com.ds.communication_service.business.v1.services;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ds.communication_service.app_context.models.*; 
import com.ds.communication_service.app_context.repositories.*; 
import com.ds.communication_service.common.dto.BaseResponse;
import com.ds.communication_service.common.dto.CreateProposalRequest;
import com.ds.communication_service.common.dto.InteractiveProposalResponseDTO;
import com.ds.communication_service.common.dto.MessageResponse; 
import com.ds.communication_service.common.dto.ProposalUpdateRequest;
import com.ds.communication_service.common.enums.ContentType;
import com.ds.communication_service.common.enums.MessageStatus;
import com.ds.communication_service.common.enums.ProposalActionType;
import com.ds.communication_service.common.enums.ProposalStatus;
import com.ds.communication_service.common.enums.ProposalType;
import com.ds.communication_service.common.interfaces.IProposalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor 
public class ProposalService implements IProposalService{

    private final InteractiveProposalRepository proposalRepo;
    private final ProposalTypeConfigRepository configRepo;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    
    private final SimpMessageSendingOperations messagingTemplate; 
    private final RestTemplate restTemplate;

    @Value("${services.session.base-url}")
    private String sessionServiceUrl;
    /**
     * Tạo một proposal mới và gửi tin nhắn.
     */
    @Transactional
    @Override
    public InteractiveProposal createProposal(CreateProposalRequest dto) {

        String senderId = dto.getSenderId();
        Collection<String> senderRoles = dto.getSenderRoles();

        // 1. Lấy cấu hình (Config) - ĐÃ CÓ ACTIONTYPE
        ProposalTypeConfig config = configRepo.findByType(dto.getType())
            .orElseThrow(() -> new IllegalArgumentException("Proposal type không hợp lệ: " + dto.getType()));
        
        // 2. Kiểm tra quyền
        if (!senderRoles.contains(config.getRequiredRole()) && !senderRoles.contains("ADMIN")) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện hành động này.");
        }
        
        // 3. Lấy Conversation
        Conversation conversation = conversationRepo.findById(dto.getConversationId())
            .orElseThrow(() -> new EntityNotFoundException("Conversation not found."));

        // 4. Tạo Proposal
        InteractiveProposal proposal = new InteractiveProposal();
        proposal.setConversation(conversation);
        proposal.setProposerId(senderId);
        proposal.setRecipientId(dto.getRecipientId());
        proposal.setType(dto.getType()); 
        proposal.setData(dto.getData());
        proposal.setStatus(ProposalStatus.PENDING);
        proposal.setSessionId(dto.getSessionId()); // Set sessionId if provided
        
        proposal.setActionType(config.getResponseActionType()); 
        
        if (config.getDefaultTimeoutMinutes() != null) {
            proposal.setExpiresAt(
                LocalDateTime.now().plusMinutes(config.getDefaultTimeoutMinutes())
            );
        }
        InteractiveProposal savedProposal = proposalRepo.save(proposal);

        // 5. Tạo Message tương ứng
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setType(ContentType.INTERACTIVE_PROPOSAL);
        message.setContent(dto.getFallbackContent());
        message.setStatus(MessageStatus.SENT); // Set initial status
        message.setSentAt(LocalDateTime.now()); // CRITICAL: Set sentAt timestamp to avoid null constraint violation
        
        message.setProposal(savedProposal); 
        Message savedMessage = messageRepo.save(message);

        // 6. GỬI SỰ KIỆN TẠO MỚI QUA WEBSOCKET
        // MessageResponse (được tạo bởi toDto) cần trả về
        // toàn bộ object 'proposal' hoặc ít nhất là 'actionType' của nó.
        log.info("Gửi sự kiện TẠO PROPOSAL đến 2 user: {} và {}", senderId, dto.getRecipientId());
        MessageResponse messageResponse = toDto(savedMessage);

        messagingTemplate.convertAndSendToUser(
            dto.getRecipientId(), "/queue/messages", messageResponse             
        );
        messagingTemplate.convertAndSendToUser(
            senderId, "/queue/messages", messageResponse             
        );
        
        log.info("Proposal {} (Type: {}) đã được tạo bởi User {}", savedProposal.getId(), dto.getType(), senderId);
        return savedProposal; 
    }

    @Transactional
    @Override
    public InteractiveProposal respondToProposal(UUID proposalId, String currentUserId, String resultData) {
        InteractiveProposal proposal = findProposalAndCheckPermissions(proposalId, currentUserId);
        // 2. Lưu kết quả
        proposal.setResultData(resultData);

        // 3. Cập nhật trạng thái
        // Đối với ACCEPT_DECLINE, client sẽ gửi "ACCEPTED" hoặc "DECLINED"
        if (proposal.getActionType() == ProposalActionType.ACCEPT_DECLINE) {
            if ("DECLINED".equals(resultData)) {
                proposal.setStatus(ProposalStatus.DECLINED);
            } else {
                // Mặc định mọi phản hồi khác (kể cả "ACCEPTED") là chấp nhận
                proposal.setStatus(ProposalStatus.ACCEPTED);
            }
        } else {
            // Đối với TEXT_INPUT, DATE_PICKER...
            // Cứ GỬI là auto ACCEPTED
            proposal.setStatus(ProposalStatus.ACCEPTED);
        }

        InteractiveProposal savedProposal = proposalRepo.save(proposal);
        
        // 4. GỬI SỰ KIỆN CẬP NHẬT QUA WEBSOCKET (Giống logic cũ)
        log.info("Gửi sự kiện RESPOND proposal {} (Status: {}) đến 2 user.", proposalId, savedProposal.getStatus());
        ProposalUpdateRequest updateDto = new ProposalUpdateRequest(
            savedProposal.getId(), 
            savedProposal.getStatus(), 
            savedProposal.getConversation().getId(),
            savedProposal.getResultData()
        );
        
        messagingTemplate.convertAndSendToUser(
            savedProposal.getRecipientId(), "/queue/proposal-updates", updateDto
        );
        messagingTemplate.convertAndSendToUser(
            savedProposal.getProposerId(), "/queue/proposal-updates", updateDto
        );
        
        // Only call external APIs if proposal is ACCEPTED
        if (savedProposal.getStatus() == ProposalStatus.ACCEPTED) {
            if (proposal.getType().equals(ProposalType.CONFIRM_REFUSAL)) {
                callRefuseParcelApi(proposal.getProposerId(), proposal.getData());
            }

            if (proposal.getType().equals(ProposalType.POSTPONE_REQUEST)) {
                // For POSTPONE_REQUEST: proposer is CLIENT, recipient is SHIPPER
                // When SHIPPER accepts, use recipientId (SHIPPER) as deliveryManId
                String deliveryManId = savedProposal.getRecipientId();
                callPostponeParcelApi(deliveryManId, proposal.getData());
            }
        }

        log.info("Proposal {} đã được PHẢN HỒI bởi User {}", proposalId, currentUserId);
        return savedProposal;
    }

    private void callRefuseParcelApi(String deliveryManId, String data) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node;
        try {
            log.info("🔍 callRefuseParcelApi - deliveryManId: {}, data: {}", deliveryManId, data);
            node = mapper.readTree(data);
            
            // Check if parcelId exists in data
            JsonNode parcelIdNode = node.get("parcelId");
            if (parcelIdNode == null || parcelIdNode.isNull()) {
                log.warn("⚠️ parcelId not found in proposal data: {}", data);
                log.warn("⚠️ Available fields in data: {}", node.fieldNames());
                return; // Skip API call if parcelId is missing
            }
            
            String parcelId = parcelIdNode.asText();
            if (parcelId == null || parcelId.isEmpty()) {
                log.warn("⚠️ parcelId is empty in proposal data: {}", data);
                return; // Skip API call if parcelId is empty
            }
            
            String url = String.format("%s/api/v1/assignments/drivers/%s/parcels/%s/refuse",
                                   sessionServiceUrl, deliveryManId, parcelId);
        
            log.info("✅ Đang gọi API ngoài: POST {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Gọi API Refuse Parcel thành công cho Parcel ID: {}", parcelId);
            } else {
                log.warn("⚠️ API Refuse Parcel trả về status code: {} cho Parcel ID: {}", 
                    response.getStatusCode(), parcelId);
            }
        } catch (JsonProcessingException e) {
            log.error("❌ Lỗi parse JSON khi gọi Refuse Parcel API. Data: {}", data, e);
            // Don't throw - this is a side effect, shouldn't fail proposal response
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi Refuse Parcel API", e);
            // Don't throw - this is a side effect, shouldn't fail proposal response
        }
    }

    private void callPostponeParcelApi(String deliveryManId, String data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);

            // Lấy parcelId ra
            JsonNode parcelIdNode = node.get("parcelId");
            if (parcelIdNode == null || parcelIdNode.isNull()) {
                log.warn("⚠️ Không tìm thấy parcelId trong dữ liệu postpone. Data: {}", data);
                log.warn("⚠️ Available fields in data: {}", node.fieldNames());
                return;
            }
            
            String parcelId = parcelIdNode.asText();
            if (parcelId == null || parcelId.isEmpty()) {
                log.warn("⚠️ parcelId is empty in postpone proposal data: {}", data);
                return;
            }

            // Step 1: Query assignmentId từ parcelId + deliveryManId
            UUID assignmentId = null;
            try {
                String queryUrl = String.format("%s/api/v1/assignments/active?parcelId=%s&deliveryManId=%s",
                        sessionServiceUrl, parcelId, deliveryManId);
                
                log.info("🔍 Querying assignmentId for parcelId: {} and deliveryManId: {}", parcelId, deliveryManId);
                ResponseEntity<String> queryResponse = restTemplate.getForEntity(queryUrl, String.class);
                
                if (queryResponse.getStatusCode().is2xxSuccessful() && queryResponse.getBody() != null) {
                    // Parse JSON response from Session Service (which uses result field)
                    JsonNode responseNode = mapper.readTree(queryResponse.getBody());
                    if (responseNode.has("result") && !responseNode.get("result").isNull()) {
                        String resultStr = responseNode.get("result").asText();
                        assignmentId = UUID.fromString(resultStr);
                        log.info("✅ Found assignmentId: {} for parcelId: {} and deliveryManId: {}", assignmentId, parcelId, deliveryManId);
                    } else {
                        log.warn("⚠️ No assignmentId found in response. Response: {}", queryResponse.getBody());
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to query assignmentId, will fallback to old endpoint: {}", e.getMessage());
            }

            // Step 2: Gọi endpoint postpone
            // Tạo payload: loại bỏ trường parcelId (và assignmentId nếu có), giữ lại reason và routeInfo
            ((ObjectNode) node).remove("parcelId");
            JsonNode assignmentIdNode = node.get("assignmentId");
            if (assignmentIdNode != null && !assignmentIdNode.isNull()) {
                ((ObjectNode) node).remove("assignmentId");
                // Use assignmentId from data if provided
                try {
                    assignmentId = UUID.fromString(assignmentIdNode.asText());
                    log.info("📋 Using assignmentId from proposal data: {}", assignmentId);
                } catch (Exception e) {
                    log.warn("⚠️ Invalid assignmentId in proposal data, using queried assignmentId");
                }
            }
            
            String reason = node.has("reason") ? node.get("reason").asText() : 
                           (node.has("resultData") ? node.get("resultData").asText() : 
                           "Khách yêu cầu hoãn");
            
            // Tạo payload cho postpone request
            ObjectNode postponePayload = mapper.createObjectNode();
            postponePayload.put("reason", reason);
            // RouteInfo sẽ null nếu không có trong data (optional)
            if (node.has("routeInfo")) {
                postponePayload.set("routeInfo", node.get("routeInfo"));
            }
            String postponeData = mapper.writeValueAsString(postponePayload);

            // Gọi endpoint mới nếu có assignmentId, otherwise fallback to old endpoint
            if (assignmentId != null) {
                // Use new endpoint with assignmentId
                String url = String.format("%s/api/v1/assignments/%s/postpone",
                        sessionServiceUrl, assignmentId);

                log.info("✅ Gọi API postpone bằng assignmentId: PUT {}", url);
                log.debug("Payload gửi đi: {}", postponeData);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(postponeData, headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ Gọi API postpone assignment thành công cho Assignment ID: {}", assignmentId);
                } else {
                    log.warn("⚠️ API postpone assignment trả về status code: {} cho Assignment ID: {}", 
                        response.getStatusCode(), assignmentId);
                }
            } else {
                // Fallback to old endpoint (backward compatibility)
                String url = String.format("%s/api/v1/assignments/drivers/%s/parcels/%s/postpone",
                        sessionServiceUrl, deliveryManId, parcelId);

                log.warn("⚠️ Không tìm thấy assignmentId, sử dụng endpoint cũ: POST {}", url);
                log.debug("Payload gửi đi: {}", postponeData);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(reason, headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ Gọi API postpone parcel thành công (fallback) cho Parcel ID: {}", parcelId);
                } else {
                    log.warn("⚠️ API postpone parcel (fallback) trả về status code: {} cho Parcel ID: {}", 
                        response.getStatusCode(), parcelId);
                }
            }

        } catch (JsonProcessingException e) {
            log.error("❌ Lỗi parse JSON trong callPostponeParcelApi. Data: {}", data, e);
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi postpone parcel API: {}", e.getMessage(), e);
        }
    }


    /**
     * Logic (chạy tự động) để xử lý các proposal hết hạn.
     */
    @Transactional
    @Scheduled(fixedRate = 60000)
    public void processExpiredProposals() {
        List<InteractiveProposal> expiredProposals = proposalRepo
            .findByStatusAndExpiresAtLessThanEqual(ProposalStatus.PENDING, LocalDateTime.now());

        if (expiredProposals.isEmpty()) {
            return;
        }

        log.info("Tìm thấy {} proposal đã hết hạn. Đang cập nhật trạng thái...", expiredProposals.size());

        for (InteractiveProposal proposal : expiredProposals) {
            proposal.setStatus(ProposalStatus.EXPIRED);
            
            // --- 5. GỬI SỰ KIỆN CẬP NHẬT QUA WEBSOCKET ---
            ProposalUpdateRequest updateDto = new ProposalUpdateRequest(
                proposal.getId(), 
                proposal.getStatus(), 
                proposal.getConversation().getId(),
                proposal.getResultData()
            );

            // Gửi đến cả 2 user
            messagingTemplate.convertAndSendToUser(
                proposal.getRecipientId(), "/queue/proposal-updates", updateDto
            );
            messagingTemplate.convertAndSendToUser(
                proposal.getProposerId(), "/queue/proposal-updates", updateDto
            );
        }
        
        proposalRepo.saveAll(expiredProposals);
    }

    private InteractiveProposal findProposalAndCheckPermissions(UUID proposalId, String currentUserId) {
        InteractiveProposal proposal = proposalRepo.findById(proposalId)
            .orElseThrow(() -> new EntityNotFoundException("Proposal not found."));
        if (!proposal.getRecipientId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không phải người nhận của đề nghị này.");
        }
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new IllegalStateException("Đề nghị này đã được xử lý hoặc hết hạn.");
        }
        return proposal;
    }
    
    private MessageResponse toDto(Message message) {        
        InteractiveProposalResponseDTO res = message.getProposal() != null ? InteractiveProposalResponseDTO.from(message.getProposal()) : null;
        return MessageResponse.builder()
            .id(message.getId().toString()) 
            .conversationId(message.getConversation() != null ? message.getConversation().getId().toString() : null) // CRITICAL: Include conversationId for Android filtering
            .content(message.getContent())
            .type(message.getType())
            .senderId(message.getSenderId())
            .sentAt(message.getSentAt())
            .status(message.getStatus()) // Include status
            .proposal(res) 
            .build();
    }
}
