package com.ds.communication_service.common.interfaces;

import java.util.List;
import java.util.UUID;

import com.ds.communication_service.app_context.models.ProposalTypeConfig;
import com.ds.communication_service.common.dto.ProposalConfigDTO;
import com.ds.communication_service.common.enums.ProposalType;

public interface IProposalConfigService {

    List<ProposalTypeConfig> getAllConfigs();

    ProposalTypeConfig getConfigByType(ProposalType type);

    ProposalTypeConfig createConfig(ProposalConfigDTO dto);

    ProposalTypeConfig updateConfig(UUID configId, ProposalConfigDTO dto);

    void deleteConfig(UUID configId);
}