package com.ds.gateway.application.controllers.v1;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/session")
@RequiredArgsConstructor
public class SessionController {

    private final RestTemplate restTemplate;

    @Value("${services.session.base-url}")
    private String sessionServiceUrl;

    @PostMapping("/tasks/{parcelId}/accept")
    public ResponseEntity<?> acceptTask(@PathVariable("parcelId") String parcelId, @RequestParam String deliveryManId) {
        String url = sessionServiceUrl + "/api/v1/assignments/" + parcelId + "/accept?deliveryManId=" + deliveryManId;
        return ResponseEntity.ok(restTemplate.postForObject(url, null, Object.class));
    }

    @PostMapping("/tasks/{taskId}/complete") // Gateway keeps POST mapping for compatibility
    public ResponseEntity<?> completeTask(@PathVariable("taskId") String taskId, @RequestParam String deliveryManId, @RequestBody Object body) {
        // Service expects: PUT /api/v1/assignments/{taskId}/complete
        String url = sessionServiceUrl + "/api/v1/assignments/" + taskId + "/complete?deliveryManId=" + deliveryManId;
        
        HttpEntity<Object> requestEntity = new HttpEntity<>(body);

        ResponseEntity<Object> response = restTemplate.exchange(
            url, 
            HttpMethod.PUT, 
            requestEntity, 
            Object.class
        );
        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/tasks/{taskId}/fail") // Gateway keeps POST mapping for compatibility
    public ResponseEntity<?> deliveryFailed(@PathVariable("taskId") String taskId, 
                                            @RequestParam String deliveryManId, 
                                            @RequestParam String reason, 
                                            @RequestParam boolean flag, 
                                            @RequestBody Object body) {
        
        // Service expects: PUT /api/v1/assignments/{taskId}/fail?deliveryManId={id}&reason={r}&flag={f}
        String url = String.format(
            "%s/api/v1/assignments/%s/fail?deliveryManId=%s&reason=%s&flag=%b",
            sessionServiceUrl, taskId, deliveryManId, reason, flag);
            
        HttpEntity<Object> requestEntity = new HttpEntity<>(body);

        ResponseEntity<Object> response = restTemplate.exchange(
            url, 
            HttpMethod.PUT, 
            requestEntity, 
            Object.class
        );
        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/delivery-man/{deliveryManId}/tasks/today")
    public ResponseEntity<?> getTasksTodayOfDeliveryMan(@PathVariable String deliveryManId,
                                                        @RequestParam(required = false) List<String> status,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        
        String url = String.format(
            "%s/api/v1/assignments/today/%s?page=%d&size=%d",
            sessionServiceUrl, deliveryManId, page, size);
        
        if (status != null && !status.isEmpty()) {
             String statusQuery = status.stream().collect(Collectors.joining("&status="));
             url += "&status=" + statusQuery;
        }

        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    @GetMapping("/delivery-man/{deliveryManId}/tasks")
    public ResponseEntity<?> getTasksOfDeliveryMan(@PathVariable String deliveryManId,
                                                   @RequestParam(required = false) List<String> status, 
                                                   @RequestParam(required = false) String createdAtStart,
                                                   @RequestParam(required = false) String createdAtEnd,
                                                   @RequestParam(required = false) String completedAtStart,
                                                   @RequestParam(required = false) String completedAtEnd,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {

        String url = String.format(
            "%s/api/v1/assignments/%s?createdAtStart=%s&createdAtEnd=%s&completedAtStart=%s&completedAtEnd=%s&page=%d&size=%d",
            sessionServiceUrl, deliveryManId, 
            createdAtStart == null ? "" : createdAtStart,
            createdAtEnd == null ? "" : createdAtEnd,
            completedAtStart == null ? "" : completedAtStart, 
            completedAtEnd == null ? "" : completedAtEnd, 
            page, size);
        
        if (status != null && !status.isEmpty()) {
             String statusQuery = status.stream().collect(Collectors.joining("&status="));
             url += "&status=" + statusQuery;
        }

        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }
}