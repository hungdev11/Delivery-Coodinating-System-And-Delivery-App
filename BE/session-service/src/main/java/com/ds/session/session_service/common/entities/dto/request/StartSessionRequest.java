package com.ds.session.session_service.common.entities.dto.request;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSessionRequest {
    private Double startLocationLat; // Vị trí bắt đầu khi shipper nhấn "Bắt đầu giao hàng"
    private Double startLocationLon;
    private LocalDateTime startLocationTimestamp;
}
