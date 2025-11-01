package com.ds.communication_service.application.configs;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ds.communication_service.app_context.models.ProposalTypeConfig;
import com.ds.communication_service.app_context.repositories.ProposalTypeConfigRepository;
import com.ds.communication_service.common.enums.ProposalType;
import com.ds.communication_service.common.enums.ProposalActionType;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ProposalConfigValidator {

    @Bean
    @Transactional
    public ApplicationRunner validateAndSeedProposalConfigs(ProposalTypeConfigRepository configRepo) {
        return args -> {
            log.info("Validating and seeding proposal configurations...");
            
            for (ProposalType type : ProposalType.values()) {
                
                if (!configRepo.existsByType(type)) {
                    
                    log.warn("!!! CẢNH BÁO: Thiếu cấu hình cho ProposalType: {}. Đang tự động tạo...", type);
                    
                    ProposalTypeConfig newConfig = new ProposalTypeConfig();
                    newConfig.setType(type);
                    
                    switch (type) {
                        case CONFIRM_REFUSAL:
                            newConfig.setRequiredRole("SHIPPER");
                            newConfig.setDescription("Shipper yêu cầu khách xác nhận từ chối đơn.");
                            newConfig.setDefaultTimeoutMinutes(15L); 
                            // Shipper (Sender) chỉ cần bấm gửi
                            newConfig.setCreationActionType(ProposalActionType.ACCEPT_DECLINE); 
                            // Khách hàng (Receiver) phải bấm Accept/Decline
                            newConfig.setResponseActionType(ProposalActionType.ACCEPT_DECLINE);
                            break;
                            
                        case POSTPONE_REQUEST:
                            newConfig.setRequiredRole("CLIENT"); 
                            newConfig.setDescription("Khách hàng yêu cầu hoãn/dời đơn.");
                            newConfig.setDefaultTimeoutMinutes(10L); 
                            // Khách hàng (Sender) phải chọn ngày
                            newConfig.setCreationActionType(ProposalActionType.DATE_PICKER); 
                            // Shipper (Receiver) phải bấm Accept/Decline
                            newConfig.setResponseActionType(ProposalActionType.ACCEPT_DECLINE); 
                            break;
                            
                        default:
                            log.error("Không có cấu hình mặc định cho {}, vui lòng cập nhật ProposalConfigValidator!", type);
                            newConfig.setRequiredRole("ADMIN"); 
                            newConfig.setDescription("Cấu hình mặc định - CẦN CẬP NHẬT");
                            newConfig.setDefaultTimeoutMinutes(5L);
                            newConfig.setCreationActionType(ProposalActionType.INFO_ONLY);
                            newConfig.setResponseActionType(ProposalActionType.INFO_ONLY); 
                    }
                    
                    configRepo.save(newConfig);
                    log.info("Đã tự động tạo cấu hình mặc định cho {}.", type);
                    
                } 
            }
            
            log.info("Proposal configurations validation/seeding successful.");
        };
    }
}

