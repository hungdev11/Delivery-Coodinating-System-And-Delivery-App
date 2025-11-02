package com.ds.gateway.application.controllers.v1;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Chịu trách nhiệm cho HÀNH ĐỘNG trên TỪNG TASK (ASSIGNMENT).
 */
@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DeliveryAssignmentController {

    private final RestTemplate restTemplate;

    @Value("${services.session.base-url}")
    private String sessionServiceUrl;

    /**
     * Lấy các task của phiên đang hoạt động (phân trang).
     * Gọi tới: GET /api/v1/assignments/session/delivery-man/{deliveryManId}/tasks/today
     */
    @GetMapping("/session/delivery-man/{deliveryManId}/tasks/today")
    public ResponseEntity<?> getDailyTasks(
        @PathVariable UUID deliveryManId,
        @RequestParam(required = false) List<String> status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        String url = String.format("%s/api/v1/assignments/session/delivery-man/%s/tasks/today", 
            sessionServiceUrl, deliveryManId);
        
        // Xây dựng URL với các query param
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("page", page)
            .queryParam("size", size);
        
        if (status != null && !status.isEmpty()) {
            // Gửi 'status' lặp lại (ví dụ: ?status=PENDING&status=PROCESSING)
            builder.queryParam("status", status.toArray());
        }

        String fullUrl = builder.toUriString();
        log.info("Gateway: Proxying 'getDailyTasks' to {}", fullUrl);
        
        return restTemplate.exchange(fullUrl, HttpMethod.GET, null, Object.class);
    }

    /**
     * Lấy lịch sử task (các phiên đã đóng) với bộ lọc.
     * Gọi tới: GET /api/v1/assignments/session/delivery-man/{deliveryManId}/tasks
     */
    @GetMapping("/session/delivery-man/{deliveryManId}/tasks")
    public ResponseEntity<?> getTasksHistory(
        @PathVariable UUID deliveryManId,
        @RequestParam(required = false) List<String> status,
        @RequestParam(required = false) String createdAtStart,
        @RequestParam(required = false) String createdAtEnd,
        @RequestParam(required = false) String completedAtStart,
        @RequestParam(required = false) String completedAtEnd,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        String url = String.format("%s/api/v1/assignments/session/delivery-man/%s/tasks", 
            sessionServiceUrl, deliveryManId);

        // Sử dụng MultiValueMap để xử lý các tham số query (bao gồm cả 'null' và list)
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", String.valueOf(page));
        params.add("size", String.valueOf(size));
        
        if (status != null && !status.isEmpty()) {
            params.addAll("status", status);
        }
        if (createdAtStart != null) params.add("createdAtStart", createdAtStart);
        if (createdAtEnd != null) params.add("createdAtEnd", createdAtEnd);
        if (completedAtStart != null) params.add("completedAtStart", completedAtStart);
        if (completedAtEnd != null) params.add("completedAtEnd", completedAtEnd);

        String fullUrl = UriComponentsBuilder.fromHttpUrl(url).queryParams(params).toUriString();
        log.info("Gateway: Proxying 'getTasksHistory' to {}", fullUrl);

        return restTemplate.exchange(fullUrl, HttpMethod.GET, null, Object.class);
    }

    /**
     * Shipper báo giao hàng THÀNH CÔNG.
     * Gọi tới: POST /api/v1/assignments/drivers/{deliveryManId}/parcels/{parcelId}/complete
     */
    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/complete")
    public ResponseEntity<?> completeTask(
        @PathVariable UUID deliveryManId,
        @PathVariable UUID parcelId,
        @Valid @RequestBody Object routeInfo // Nhận JSON và chuyển tiếp
    ) {
        String url = String.format("%s/api/v1/assignments/drivers/%s/parcels/%s/complete", 
            sessionServiceUrl, deliveryManId, parcelId);
        log.info("Gateway: Proxying 'completeTask' to {}", url);
        
        HttpEntity<Object> requestEntity = new HttpEntity<>(routeInfo);
        
        return restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
    }

    /**
     * Shipper báo giao hàng THẤT BẠI.
     * Gọi tới: POST /api/v1/assignments/drivers/{deliveryManId}/parcels/{parcelId}/fail
     */
    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/fail")
    public ResponseEntity<?> failTask(
        @PathVariable UUID deliveryManId,
        @PathVariable UUID parcelId,
        @Valid @RequestBody Object taskFailRequest // Nhận JSON (TaskFailRequest)
    ) {
        String url = String.format("%s/api/v1/assignments/drivers/%s/parcels/%s/fail", 
            sessionServiceUrl, deliveryManId, parcelId);
        log.info("Gateway: Proxying 'failTask' to {}", url);

        HttpEntity<Object> requestEntity = new HttpEntity<>(taskFailRequest);

        return restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
    }

    /**
     * Shipper báo khách TỪ CHỐI nhận hàng.
     * Gọi tới: POST /api/v1/assignments/drivers/{deliveryManId}/parcels/{parcelId}/refuse
     */
    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/refuse")
    public ResponseEntity<?> refuseTask(
        @PathVariable UUID deliveryManId,
        @PathVariable UUID parcelId
    ) {
        String url = String.format("%s/api/v1/assignments/drivers/%s/parcels/%s/refuse", 
            sessionServiceUrl, deliveryManId, parcelId);
        log.info("Gateway: Proxying 'refuseTask' to {}", url);
        
        return restTemplate.exchange(url, HttpMethod.POST, null, Object.class);
    }

    /**
     * Khách báo hoãn
     * Gọi tới: POST /api/v1/assignments/drivers/{deliveryManId}/parcels/{parcelId}/postpone
     */
    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/postpone")
    public ResponseEntity<?> postponeTask(
        @PathVariable UUID deliveryManId,
        @PathVariable UUID parcelId,
        @RequestBody String addInfo
    ) {
        String url = String.format("%s/api/v1/assignments/drivers/%s/parcels/%s/postpone", 
            sessionServiceUrl, deliveryManId, parcelId);
        log.info("Gateway: Proxying 'refuseTask' to {}", url);
        
        HttpEntity<Object> requestEntity = new HttpEntity<>(addInfo);

        return restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
    }

    @GetMapping("/current-shipper/parcels/{parcelId}")
    ResponseEntity<?> lastestShipperForParcel(
        @PathVariable UUID parcelId
    ) {
        String url = String.format("%s/api/v1/assignments/current-shipper/parcels/%s", 
            sessionServiceUrl, parcelId);
        log.info("Gateway: Proxying 'lastest shipper' to {}", url);
        
        return restTemplate.exchange(url, HttpMethod.GET, null, Object.class);
    }
}
