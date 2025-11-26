package com.ds.user.application.controllers.internal;

import com.ds.user.app_context.repositories.DeliveryManRepository;
import com.ds.user.common.entities.base.DeliveryMan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal endpoint for dumping all delivery men for snapshot initialization
 * Used by session-service to load initial delivery man data into snapshot table
 */
@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class DeliveryManDumpController {

    private final DeliveryManRepository deliveryManRepository;

    /**
     * Dump all delivery men for snapshot initialization
     * Returns paginated results to handle large datasets
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 1000, max: 10000)
     * @return List of delivery men with pagination info
     */
    @GetMapping("/delivery-man-dump")
    public ResponseEntity<Map<String, Object>> getDeliveryManDump(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size) {
        
        // Limit max page size to prevent memory issues
        int pageSize = Math.min(size, 10000);
        
        log.info("📦 DeliveryMan dump requested: page={}, size={}", page, pageSize);
        
        try {
            // Get delivery men using pagination
            Pageable pageable = PageRequest.of(page, pageSize);
            Page<DeliveryMan> dmPage = deliveryManRepository.findAll(pageable);
            
            // Convert to DTO format for snapshot
            List<Map<String, Object>> deliveryMen = new ArrayList<>();
            for (DeliveryMan dm : dmPage.getContent()) {
                Map<String, Object> dmData = new HashMap<>();
                dmData.put("userId", dm.getUser().getId());
                dmData.put("username", dm.getUser().getUsername());
                dmData.put("email", dm.getUser().getEmail());
                dmData.put("firstName", dm.getUser().getFirstName());
                dmData.put("lastName", dm.getUser().getLastName());
                dmData.put("phone", dm.getUser().getPhone());
                dmData.put("status", dm.getUser().getStatus() != null ? dm.getUser().getStatus().name() : null);
                dmData.put("vehicleType", dm.getVehicleType());
                dmData.put("capacityKg", dm.getCapacityKg());
                dmData.put("createdAt", dm.getCreatedAt() != null ? dm.getCreatedAt().toString() : null);
                dmData.put("updatedAt", dm.getUpdatedAt() != null ? dm.getUpdatedAt().toString() : null);
                deliveryMen.add(dmData);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("deliveryMen", deliveryMen);
            response.put("page", dmPage.getNumber());
            response.put("size", dmPage.getSize());
            response.put("totalElements", dmPage.getTotalElements());
            response.put("totalPages", dmPage.getTotalPages());
            response.put("hasNext", dmPage.hasNext());
            response.put("hasPrevious", dmPage.hasPrevious());
            
            log.info("✅ DeliveryMan dump completed: {} delivery men returned (page {}/{})", 
                deliveryMen.size(), page + 1, dmPage.getTotalPages());
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(response);
                    
        } catch (Exception e) {
            log.error("❌ Error generating delivery man dump: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate delivery man dump: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(errorResponse);
        }
    }
}
