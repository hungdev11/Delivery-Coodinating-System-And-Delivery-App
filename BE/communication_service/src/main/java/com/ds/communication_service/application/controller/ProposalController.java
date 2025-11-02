package com.ds.communication_service.application.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ds.communication_service.app_context.models.InteractiveProposal;
import com.ds.communication_service.app_context.models.ProposalTypeConfig;
import com.ds.communication_service.app_context.repositories.ProposalTypeConfigRepository;
import com.ds.communication_service.common.dto.CreateProposalRequest;
import com.ds.communication_service.common.dto.InteractiveProposalResponseDTO;
import com.ds.communication_service.common.dto.ProposalResponseRequest;
import com.ds.communication_service.common.interfaces.IProposalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/proposals")
public class ProposalController {

    // Inject Interface thay vì class cụ thể
    private final IProposalService proposalService;
    private final ProposalTypeConfigRepository configRepository;
    /**
     * Endpoint để tạo một proposal mới (ví dụ: Shipper yêu cầu hủy).
     */
    @PostMapping
    public ResponseEntity<InteractiveProposalResponseDTO> createProposal(
            @RequestBody CreateProposalRequest dto
    ) {
        InteractiveProposal proposal = proposalService.createProposal(dto);
        return ResponseEntity.ok(InteractiveProposalResponseDTO.from(proposal));
    }

    @PostMapping("/{proposalId}/respond")
    public ResponseEntity<InteractiveProposalResponseDTO> respondToProposal(
            @PathVariable UUID proposalId,
            @RequestParam String userId,
            @RequestBody ProposalResponseRequest dto // DTO chứa { "resultData": "..." }
    ) {
        InteractiveProposal proposal = proposalService.respondToProposal(
            proposalId, 
            userId, 
            dto.getResultData() 
        );
        return ResponseEntity.ok(InteractiveProposalResponseDTO.from(proposal));
    }

    @GetMapping("/available-configs")
    public ResponseEntity<List<ProposalTypeConfig>> getAvailableConfigs(@RequestParam List<String> roles
    ) {
        roles.forEach(r -> log.info(r));
        List<ProposalTypeConfig> availableConfigs = 
                configRepository.findByRequiredRoleIn(roles);
                
        return ResponseEntity.ok(availableConfigs);
    }
}