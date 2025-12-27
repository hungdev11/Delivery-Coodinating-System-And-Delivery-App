package com.ds.session.session_service.common.entities.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for bulk query of delivery man session information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDeliveryManSessionInfoRequest {
    /**
     * List of delivery man user IDs to query session info for
     */
    private List<String> deliveryManIds;
}
