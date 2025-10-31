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
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ds.communication_service.app_context.models.*; 
import com.ds.communication_service.app_context.repositories.*; 
import com.ds.communication_service.common.dto.CreateProposalRequest;
import com.ds.communication_service.common.dto.InteractiveProposalResponseDTO;
import com.ds.communication_service.common.dto.MessageResponse; 
import com.ds.communication_service.common.dto.ProposalUpdateRequest;
import com.ds.communication_service.common.enums.ContentType;
import com.ds.communication_service.common.enums.ProposalActionType;
import com.ds.communication_service.common.enums.ProposalStatus;
import com.ds.communication_service.common.enums.ProposalType;
import com.ds.communication_service.common.interfaces.IProposalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        if (!senderRoles.contains(config.getRequiredRole())) {
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
        
        if (proposal.getType().equals(ProposalType.CONFIRM_REFUSAL)) {
            callRefuseParcelApi(proposal.getProposerId(), proposal.getData());
        }

        log.info("Proposal {} đã được PHẢN HỒI bởi User {}", proposalId, currentUserId);
        return savedProposal;
    }

    private void callRefuseParcelApi(String deliveryManId, String data) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node;
        try {
            node = mapper.readTree(data);
            String parcelId = node.get("parcelId").asText();
            String url = String.format("%s/api/v1/assignments/drivers/%s/parcels/%s/refuse",
                                   sessionServiceUrl, deliveryManId, parcelId);
        
            log.info("Đang gọi API ngoài: POST {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            log.info("Gọi API Refuse Parcel thành công cho Parcel ID: {}", parcelId);
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
            .content(message.getContent())
            .type(message.getType())
            .senderId(message.getSenderId())
            .sentAt(message.getSentAt())
            .proposal(res) 
            .build();
    }
}