package com.ds.gateway.application.controllers.v1;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Chịu trách nhiệm cho VÒNG ĐỜI CỦA PHIÊN (SESSION).
 */
@RestController
@RequestMapping("/api/v1/sessions") 
@RequiredArgsConstructor
@Slf4j
@Validated
public class SessionController {

    private final RestTemplate restTemplate;

    @Value("${services.session.base-url}")
    private String sessionServiceUrl;

    /**
     * API Quét-để-thêm-task (Scan-to-add).
     * Gọi tới: POST /api/v1/sessions/drivers/{deliveryManId}/accept-parcel
     */
    @PostMapping("/drivers/{deliveryManId}/accept-parcel")
    public ResponseEntity<?> acceptParcelToSession(
            @PathVariable("deliveryManId") String deliveryManId,
            @Valid @RequestBody Object scanParcelRequest // Nhận JSON và chuyển tiếp
    ) {
        String url = String.format("%s/api/v1/sessions/drivers/%s/accept-parcel", 
            sessionServiceUrl, deliveryManId);
        log.info("Gateway: Proxying 'acceptParcel' to {}", url);
        
        HttpEntity<Object> requestEntity = new HttpEntity<>(scanParcelRequest);
        
        return restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
    }

    /**
     * Lấy thông tin chi tiết một phiên.
     * Gọi tới: GET /api/v1/sessions/{sessionId}
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSessionById(@PathVariable UUID sessionId) {
        String url = String.format("%s/api/v1/sessions/%s", sessionServiceUrl, sessionId);
        log.info("Gateway: Proxying 'getSessionById' to {}", url);
        
        return restTemplate.exchange(url, HttpMethod.GET, null, Object.class);
    }

    /**
     * Shipper chủ động hoàn thành phiên.
     * Gọi tới: POST /api/v1/sessions/{sessionId}/complete
     */
    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<?> completeSession(@PathVariable UUID sessionId) {
        String url = String.format("%s/api/v1/sessions/%s/complete", sessionServiceUrl, sessionId);
        log.info("Gateway: Proxying 'completeSession' to {}", url);

        return restTemplate.exchange(url, HttpMethod.POST, null, Object.class);
    }

    /**
     * Shipper báo cáo sự cố (hủy phiên).
     * Gọi tới: POST /api/v1/sessions/{sessionId}/fail
     */
    @PostMapping("/{sessionId}/fail")
    public ResponseEntity<?> failSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody Object sessionFailRequest
    ) {
        String url = String.format("%s/api/v1/sessions/%s/fail", sessionServiceUrl, sessionId);
        log.warn("Gateway: Proxying 'failSession' to {}", url);
        
        HttpEntity<Object> requestEntity = new HttpEntity<>(sessionFailRequest);
        
        return restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
    }

    /**
     * (Tùy chọn) Tạo phiên hàng loạt.
     * Gọi tới: POST /api/v1/sessions
     */
    @PostMapping
    public ResponseEntity<?> createSessionBatch(
            @Valid @RequestBody Object createSessionRequest
    ) {
        String url = String.format("%s/api/v1/sessions", sessionServiceUrl);
        log.info("Gateway: Proxying 'createSessionBatch' to {}", url);
        
        HttpEntity<Object> requestEntity = new HttpEntity<>(createSessionRequest);
        
        return restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
    }
}
