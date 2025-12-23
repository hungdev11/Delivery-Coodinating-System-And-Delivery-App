package com.ds.deliveryapp.clients.req;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CompleteTaskRequest {
    private RouteInfo routeInfo;
    private List<String> proofImageUrls; // Danh sách URL từ Cloudinary
    private Double currentLat; // Vị trí hiện tại của shipper khi xác nhận
    private Double currentLon;
    /**
     * ISO-8601 string, parsed as LocalDateTime on backend
     */
    private String currentTimestamp;
}
