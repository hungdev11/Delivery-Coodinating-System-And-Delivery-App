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
        log.debug("[communication-service] [ProposalService.createProposal] Gửi sự kiện TẠO PROPOSAL đến 2 user: {} và {}", senderId, dto.getRecipientId());
        MessageResponse messageResponse = toDto(savedMessage);

        messagingTemplate.convertAndSendToUser(
            dto.getRecipientId(), "/queue/messages", messageResponse             
        );
        messagingTemplate.convertAndSendToUser(
            senderId, "/queue/messages", messageResponse             
        );
        
        log.debug("[communication-service] [ProposalService.createProposal] Proposal {} (Type: {}) đã được tạo bởi User {}", savedProposal.getId(), dto.getType(), senderId);
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
        log.debug("[communication-service] [ProposalService.respondToProposal] Gửi sự kiện RESPOND proposal {} (Status: {}) đến 2 user.", proposalId, savedProposal.getStatus());
        
        // Null check for conversation to prevent NullPointerException
        Conversation conversation = savedProposal.getConversation();
        if (conversation == null) {
            log.error("[communication-service] [ProposalService.respondToProposal] Proposal {} has null conversation. Cannot send WebSocket update.", proposalId);
            throw new IllegalStateException("Proposal " + proposalId + " has no associated conversation");
        }
        
        ProposalUpdateRequest updateDto = new ProposalUpdateRequest(
            savedProposal.getId(), 
            savedProposal.getStatus(), 
            conversation.getId(),
            savedProposal.getResultData()
        );
        
        // Send WebSocket messages with error handling (don't fail transaction if WebSocket fails)
        try {
            messagingTemplate.convertAndSendToUser(
                savedProposal.getRecipientId(), "/queue/proposal-updates", updateDto
            );
            log.debug("[communication-service] [ProposalService.respondToProposal] Proposal update sent to recipient: {}", savedProposal.getRecipientId());
        } catch (Exception e) {
            log.error("[communication-service] [ProposalService.respondToProposal] Failed to send proposal update to recipient {}", savedProposal.getRecipientId(), e);
            // Don't throw - WebSocket failure shouldn't fail the transaction
        }
        
        try {
            messagingTemplate.convertAndSendToUser(
                savedProposal.getProposerId(), "/queue/proposal-updates", updateDto
            );
            log.debug("[communication-service] [ProposalService.respondToProposal] Proposal update sent to proposer: {}", savedProposal.getProposerId());
        } catch (Exception e) {
            log.error("[communication-service] [ProposalService.respondToProposal] Failed to send proposal update to proposer {}", savedProposal.getProposerId(), e);
            // Don't throw - WebSocket failure shouldn't fail the transaction
        }
        
        // Only call external APIs if proposal is ACCEPTED
        if (savedProposal.getStatus() == ProposalStatus.ACCEPTED) {
            if (proposal.getType().equals(ProposalType.CONFIRM_REFUSAL)) {
                callRefuseParcelApi(proposal.getProposerId(), proposal.getData());
            }

            if (proposal.getType().equals(ProposalType.POSTPONE_REQUEST)) {
                // For POSTPONE_REQUEST: proposer is CLIENT, recipient is SHIPPER
                // When SHIPPER accepts, use recipientId (SHIPPER) as deliveryManId
                String deliveryManId = savedProposal.getRecipientId();
                
                // Debug logging
                log.debug("[communication-service] [ProposalService.respondToProposal] Processing POSTPONE_REQUEST proposal. ProposalId: {}, DeliveryManId: {}", 
                    savedProposal.getId(), deliveryManId);
                log.debug("[communication-service] [ProposalService.respondToProposal] Original proposal data: {}", savedProposal.getData());
                log.debug("[communication-service] [ProposalService.respondToProposal] ResultData from shipper: {}", savedProposal.getResultData());
                
                // Merge original data (contains parcelId) with resultData (contains postponeDateTime from shipper)
                String mergedData = mergeProposalData(savedProposal.getData(), savedProposal.getResultData());
                log.debug("[communication-service] [ProposalService.respondToProposal] Merged data: {}", mergedData);
                
                callPostponeParcelApi(deliveryManId, mergedData);
            }
        }

        log.debug("[communication-service] [ProposalService.respondToProposal] Proposal {} đã được PHẢN HỒI bởi User {}", proposalId, currentUserId);
        return savedProposal;
    }

    private void callRefuseParcelApi(String deliveryManId, String data) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node;
        try {
            log.debug("[communication-service] [ProposalService.callRefuseParcelApi] callRefuseParcelApi - deliveryManId: {}, data: {}", deliveryManId, data);
            node = mapper.readTree(data);
            
            // Check if parcelId exists in data
            JsonNode parcelIdNode = node.get("parcelId");
            if (parcelIdNode == null || parcelIdNode.isNull()) {
                log.debug("[communication-service] [ProposalService.callRefuseParcelApi] parcelId not found in proposal data: {}", data);
                log.debug("[communication-service] [ProposalService.callRefuseParcelApi] Available fields in data: {}", node.fieldNames());
                return; // Skip API call if parcelId is missing
            }
            
            String parcelId = parcelIdNode.asText();
            if (parcelId == null || parcelId.isEmpty()) {
                log.debug("[communication-service] [ProposalService.callRefuseParcelApi] parcelId is empty in proposal data: {}", data);
                return; // Skip API call if parcelId is empty
            }
            
            String url = String.format("%s/api/v1/assignments/drivers/%s/parcels/%s/refuse",
                                   sessionServiceUrl, deliveryManId, parcelId);
        
            log.debug("[communication-service] [ProposalService.callRefuseParcelApi] Đang gọi API ngoài: POST {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("[communication-service] [ProposalService.callRefuseParcelApi] Gọi API Refuse Parcel thành công cho Parcel ID: {}", parcelId);
            } else {
                log.debug("[communication-service] [ProposalService.callRefuseParcelApi] API Refuse Parcel trả về status code: {} cho Parcel ID: {}", 
                    response.getStatusCode(), parcelId);
            }
        } catch (JsonProcessingException e) {
            log.error("[communication-service] [ProposalService.callRefuseParcelApi] Lỗi parse JSON khi gọi Refuse Parcel API. Data: {}", data, e);
            // Don't throw - this is a side effect, shouldn't fail proposal response
        } catch (Exception e) {
            log.error("[communication-service] [ProposalService.callRefuseParcelApi] Lỗi khi gọi Refuse Parcel API", e);
            // Don't throw - this is a side effect, shouldn't fail proposal response
        }
    }

    private void callPostponeParcelApi(String deliveryManId, String data) {
        try {
            log.debug("[communication-service] [ProposalService.callPostponeParcelApi] callPostponeParcelApi - deliveryManId: {}, data: {}", deliveryManId, data);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);

            // Debug: Log all fields in data
            log.debug("[communication-service] [ProposalService.callPostponeParcelApi] All fields in merged data: {}", node.fieldNames());
            
            // Lấy parcelId ra
            JsonNode parcelIdNode = node.get("parcelId");
            if (parcelIdNode == null || parcelIdNode.isNull()) {
                log.error("[communication-service] [ProposalService.callPostponeParcelApi] Không tìm thấy parcelId trong dữ liệu postpone. Data: {}", data);
                java.util.List<String> fieldNames = new java.util.ArrayList<>();
                node.fieldNames().forEachRemaining(fieldNames::add);
                log.error("[communication-service] [ProposalService.callPostponeParcelApi] Available fields in data: {}", fieldNames);
                return;
            }
            
            String parcelId = parcelIdNode.asText();
            if (parcelId == null || parcelId.isEmpty()) {
                log.error("[communication-service] [ProposalService.callPostponeParcelApi] parcelId is empty in postpone proposal data: {}", data);
                return;
            }
            
            // Validate UUID format
            try {
                UUID.fromString(parcelId);
                log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Extracted parcelId (valid UUID): {} from proposal data", parcelId);
            } catch (IllegalArgumentException e) {
                log.error("[communication-service] [ProposalService.callPostponeParcelApi] parcelId is not a valid UUID: {}", parcelId);
                return;
            }
            
            // Validate deliveryManId format
            try {
                UUID.fromString(deliveryManId);
                log.debug("[communication-service] [ProposalService.callPostponeParcelApi] deliveryManId (valid UUID): {}", deliveryManId);
            } catch (IllegalArgumentException e) {
                log.error("[communication-service] [ProposalService.callPostponeParcelApi] deliveryManId is not a valid UUID: {}", deliveryManId);
                return;
            }

            // Step 1: Query assignmentId từ parcelId + deliveryManId
            UUID assignmentId = null;
            try {
                String queryUrl = String.format("%s/api/v1/assignments/active?parcelId=%s&deliveryManId=%s",
                        sessionServiceUrl, parcelId, deliveryManId);
                
                log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Querying assignmentId for parcelId: {} and deliveryManId: {}", parcelId, deliveryManId);
                ResponseEntity<String> queryResponse = restTemplate.getForEntity(queryUrl, String.class);
                
                if (queryResponse.getStatusCode().is2xxSuccessful() && queryResponse.getBody() != null) {
                    log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Raw response from getActiveAssignmentId: {}", queryResponse.getBody());
                    
                    // Parse JSON response from Session Service (which uses result field)
                    JsonNode responseNode = mapper.readTree(queryResponse.getBody());
                    java.util.List<String> responseKeys = new java.util.ArrayList<>();
                    responseNode.fieldNames().forEachRemaining(responseKeys::add);
                    log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Parsed response node keys: {}", responseKeys);
                    
                    if (responseNode.has("result") && !responseNode.get("result").isNull()) {
                        JsonNode resultNode = responseNode.get("result");
                        log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Result node type: {}, value: {}", resultNode.getNodeType(), resultNode);
                        
                        // Handle both string and UUID object formats
                        if (resultNode.isTextual()) {
                            String resultStr = resultNode.asText();
                            log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Result is textual: {}", resultStr);
                            assignmentId = UUID.fromString(resultStr);
                        } else if (resultNode.isObject() && resultNode.has("uuid")) {
                            // If result is an object with uuid field
                            log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Result is object with uuid field: {}", resultNode.get("uuid"));
                            assignmentId = UUID.fromString(resultNode.get("uuid").asText());
                        } else {
                            // Try to parse as UUID directly
                            try {
                                log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Attempting to parse result as UUID directly: {}", resultNode);
                                assignmentId = UUID.fromString(resultNode.asText());
                            } catch (Exception e) {
                                log.error("[communication-service] [ProposalService.callPostponeParcelApi] Failed to parse assignmentId from result. Result node: {}", resultNode, e);
                            }
                        }
                        if (assignmentId != null) {
                            log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Found assignmentId: {} for parcelId: {} and deliveryManId: {}", assignmentId, parcelId, deliveryManId);
                        } else {
                            log.error("[communication-service] [ProposalService.callPostponeParcelApi] assignmentId is null after parsing. Result node: {}", resultNode);
                        }
                    } else {
                        log.error("[communication-service] [ProposalService.callPostponeParcelApi] No assignmentId found in response. Response body: {}", queryResponse.getBody());
                        log.error("[communication-service] [ProposalService.callPostponeParcelApi] Response has 'result' field: {}, isNull: {}", 
                            responseNode.has("result"), 
                            responseNode.has("result") ? responseNode.get("result").isNull() : "N/A");
                    }
                } else {
                    log.error("[communication-service] [ProposalService.callPostponeParcelApi] Query assignmentId API returned non-2xx status: {} or empty body. Status: {}, Body: {}", 
                        queryResponse.getStatusCode(), 
                        queryResponse.getStatusCode(),
                        queryResponse.getBody());
                }
            } catch (Exception e) {
                log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Failed to query assignmentId, will fallback to old endpoint: {}", e.getMessage());
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
                    log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Using assignmentId from proposal data: {}", assignmentId);
                } catch (Exception e) {
                    log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Invalid assignmentId in proposal data, using queried assignmentId");
                }
            }
            
            String reason = node.has("reason") ? node.get("reason").asText() : 
                           (node.has("resultData") ? node.get("resultData").asText() : 
                           "Khách yêu cầu hoãn");
            
            // Parse postpone datetime from proposal data
            // Support: specific_datetime, after_datetime, before_datetime, start_datetime (for RANGE)
            java.time.LocalDateTime postponeDateTime = null;
            try {
                if (node.has("specific_datetime") && !node.get("specific_datetime").isNull()) {
                    String datetimeStr = node.get("specific_datetime").asText();
                    postponeDateTime = java.time.LocalDateTime.parse(datetimeStr);
                } else if (node.has("after_datetime") && !node.get("after_datetime").isNull()) {
                    String datetimeStr = node.get("after_datetime").asText();
                    postponeDateTime = java.time.LocalDateTime.parse(datetimeStr);
                } else if (node.has("before_datetime") && !node.get("before_datetime").isNull()) {
                    String datetimeStr = node.get("before_datetime").asText();
                    postponeDateTime = java.time.LocalDateTime.parse(datetimeStr);
                } else if (node.has("start_datetime") && !node.get("start_datetime").isNull()) {
                    // For RANGE, use start_datetime
                    String datetimeStr = node.get("start_datetime").asText();
                    postponeDateTime = java.time.LocalDateTime.parse(datetimeStr);
                }
            } catch (Exception e) {
                log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Failed to parse postpone datetime from proposal data: {}", e.getMessage());
            }
            
            // Determine moveToEnd flag: if postpone is within session time, move to end
            // We'll let session-service calculate and decide based on postponeDateTime
            Boolean moveToEnd = null; // Let session-service decide based on postponeDateTime
            
            // Tạo payload cho postpone request (PostponeAssignmentRequest)
            ObjectNode postponePayload = mapper.createObjectNode();
            postponePayload.put("reason", reason);
            if (postponeDateTime != null) {
                postponePayload.put("postponeDateTime", postponeDateTime.toString());
            }
            if (moveToEnd != null) {
                postponePayload.put("moveToEnd", moveToEnd);
            }
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

                log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Gọi API postpone bằng assignmentId: PUT {}", url);
                log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Payload gửi đi: {}", postponeData);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(postponeData, headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Gọi API postpone assignment thành công cho Assignment ID: {}", assignmentId);
                } else {
                    log.error("[communication-service] [ProposalService.callPostponeParcelApi] API postpone assignment trả về status code: {} cho Assignment ID: {}. Response body: {}", 
                        response.getStatusCode(), assignmentId, response.getBody());
                    // Don't throw - this is a side effect, shouldn't fail proposal response
                }
            } else {
                // Fallback to old endpoint (backward compatibility)
                String url = String.format("%s/api/v1/assignments/drivers/%s/parcels/%s/postpone",
                        sessionServiceUrl, deliveryManId, parcelId);

                log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Không tìm thấy assignmentId, sử dụng endpoint cũ: POST {}", url);
                log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Payload gửi đi: {}", postponeData);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(reason, headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.debug("[communication-service] [ProposalService.callPostponeParcelApi] Gọi API postpone parcel thành công (fallback) cho Parcel ID: {}", parcelId);
                } else {
                    log.error("[communication-service] [ProposalService.callPostponeParcelApi] API postpone parcel (fallback) trả về status code: {} cho Parcel ID: {}. Response body: {}", 
                        response.getStatusCode(), parcelId, response.getBody());
                    // Don't throw - this is a side effect, shouldn't fail proposal response
                }
            }

        } catch (JsonProcessingException e) {
            log.error("[communication-service] [ProposalService.callPostponeParcelApi] Lỗi parse JSON trong callPostponeParcelApi. Data: {}", data, e);
            // Don't throw - this is a side effect, shouldn't fail proposal response
        } catch (Exception e) {
            log.error("[communication-service] [ProposalService.callPostponeParcelApi] Lỗi khi gọi postpone parcel API", e);
            // Don't throw - this is a side effect, shouldn't fail proposal response
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

        log.debug("[communication-service] [ProposalService.expireProposals] Tìm thấy {} proposal đã hết hạn. Đang cập nhật trạng thái...", expiredProposals.size());

        for (InteractiveProposal proposal : expiredProposals) {
            proposal.setStatus(ProposalStatus.EXPIRED);
            
            // --- 5. GỬI SỰ KIỆN CẬP NHẬT QUA WEBSOCKET ---
            // Null check for conversation to prevent NullPointerException
            Conversation conversation = proposal.getConversation();
            if (conversation == null) {
                log.error("[communication-service] [ProposalService.expireProposals] Proposal {} has null conversation. Skipping WebSocket update.", proposal.getId());
                continue; // Skip this proposal and continue with others
            }
            
            ProposalUpdateRequest updateDto = new ProposalUpdateRequest(
                proposal.getId(), 
                proposal.getStatus(), 
                conversation.getId(),
                proposal.getResultData()
            );

            // Gửi đến cả 2 user with error handling
            try {
                messagingTemplate.convertAndSendToUser(
                    proposal.getRecipientId(), "/queue/proposal-updates", updateDto
                );
                log.debug("[communication-service] [ProposalService.expireProposals] Expired proposal update sent to recipient: {}", proposal.getRecipientId());
            } catch (Exception e) {
                log.error("[communication-service] [ProposalService.expireProposals] Failed to send expired proposal update to recipient {}", proposal.getRecipientId(), e);
                // Continue with other proposals even if one fails
            }
            
            try {
                messagingTemplate.convertAndSendToUser(
                    proposal.getProposerId(), "/queue/proposal-updates", updateDto
                );
                log.debug("[communication-service] [ProposalService.expireProposals] Expired proposal update sent to proposer: {}", proposal.getProposerId());
            } catch (Exception e) {
                log.error("[communication-service] [ProposalService.expireProposals] Failed to send expired proposal update to proposer {}", proposal.getProposerId(), e);
                // Continue with other proposals even if one fails
            }
        }
        
        proposalRepo.saveAll(expiredProposals);
    }

    private InteractiveProposal findProposalAndCheckPermissions(UUID proposalId, String currentUserId) {
        // Use fetch join to ensure conversation is loaded within transaction
        InteractiveProposal proposal = proposalRepo.findByIdWithConversation(proposalId)
            .orElseThrow(() -> new EntityNotFoundException("Proposal not found."));
        if (!proposal.getRecipientId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không phải người nhận của đề nghị này.");
        }
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new IllegalStateException("Đề nghị này đã được xử lý hoặc hết hạn.");
        }
        return proposal;
    }
    
    /**
     * Merge original proposal data (contains parcelId) with resultData (contains postponeDateTime from shipper).
     * This ensures we have both parcelId and postponeDateTime when calling postpone API.
     */
    private String mergeProposalData(String originalData, String resultData) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode merged = mapper.createObjectNode();
            
            // Parse original data (contains parcelId)
            if (originalData != null && !originalData.isEmpty()) {
                try {
                    JsonNode originalNode = mapper.readTree(originalData);
                    merged.setAll((ObjectNode) originalNode);
                } catch (Exception e) {
                    log.debug("[communication-service] [ProposalService.mergeProposalData] Failed to parse original proposal data: {}", e.getMessage());
                }
            }
            
            // Merge resultData (contains postponeDateTime and other fields from shipper)
            if (resultData != null && !resultData.isEmpty()) {
                try {
                    // Check if resultData is JSON or plain string
                    if (resultData.trim().startsWith("{")) {
                        JsonNode resultNode = mapper.readTree(resultData);
                        merged.setAll((ObjectNode) resultNode);
                    } else {
                        // If resultData is plain string (e.g., "ACCEPTED"), treat it as reason
                        merged.put("reason", resultData);
                    }
                } catch (Exception e) {
                    log.debug("[communication-service] [ProposalService.mergeProposalData] Failed to parse resultData, treating as reason: {}", e.getMessage());
                    merged.put("reason", resultData);
                }
            }
            
            return mapper.writeValueAsString(merged);
        } catch (Exception e) {
            log.error("[communication-service] [ProposalService.mergeProposalData] Failed to merge proposal data. Using original data", e);
            return originalData != null ? originalData : "{}";
        }
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
