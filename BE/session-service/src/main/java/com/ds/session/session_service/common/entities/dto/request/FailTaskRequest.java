package com.ds.session.session_service.common.entities.dto.request;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailTaskRequest {
    private String failReason;
    private RouteInfo routeInfo;
    private List<String> proofImageUrls;
    private Double currentLat; // Vị trí hiện tại của shipper khi xác nhận
    private Double currentLon;
    private LocalDateTime currentTimestamp;
}
