package com.ds.gateway.application.controllers.v1;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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


    @PostMapping("/tasks")
    public ResponseEntity<?> createTask(@RequestBody Object body) {
        String url = sessionServiceUrl + "/api/v1/tasks";
        return ResponseEntity.ok(restTemplate.postForObject(url, body, Object.class));
    }

    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable String taskId, @RequestBody Object body) {
        String url = sessionServiceUrl + "/api/v1/tasks/" + taskId;
        restTemplate.put(url, body);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable String taskId) {
        String url = sessionServiceUrl + "/api/v1/tasks/" + taskId;
        restTemplate.delete(url);
        return ResponseEntity.noContent().build();
    }

    /**
     * Proxy API qua session-service để lấy thông tin Task theo ID
     */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<?> getTaskById(@PathVariable String taskId) {
        log.info("Gateway proxy -> session-service: GET /api/v1/tasks/{}", taskId);

        String url = sessionServiceUrl + "/api/v1/tasks/" + taskId;

        Object response = restTemplate.getForObject(url, Object.class);

        return ResponseEntity.ok(response);
    }

    /**
     * Proxy API qua session-service để lấy danh sách tất cả Task
     */
    @GetMapping("/tasks")
    public ResponseEntity<?> getAllTasks() {
        log.info("Gateway proxy -> session-service: GET /api/v1/tasks");

        String url = sessionServiceUrl + "/api/v1/tasks";

        Object response = restTemplate.getForObject(url, Object.class);

        return ResponseEntity.ok(response);
    }


    @PostMapping("/{taskId}/accept")
    public ResponseEntity<?> acceptTask(@PathVariable String taskId, @RequestParam String deliveryManId) {
        String url = sessionServiceUrl + "/api/v1/tasks/" + taskId + "/accept?deliveryManId=" + deliveryManId;
        return ResponseEntity.ok(restTemplate.postForObject(url, null, Object.class));
    }

    @PostMapping("/{taskId}/complete")
    public ResponseEntity<?> completeTask(@PathVariable String taskId, @RequestParam String deliveryManId, @RequestBody Object body) {
        String url = sessionServiceUrl + "/api/v1/tasks/" + taskId + "/complete?deliveryManId=" + deliveryManId;
        return ResponseEntity.ok(restTemplate.postForObject(url, body, Object.class));
    }

    @PostMapping("/{taskId}/fail")
    public ResponseEntity<?> deliveryFailed(@PathVariable String taskId, @RequestParam String deliveryManId, @RequestBody Object body) {
        String url = sessionServiceUrl + "/api/v1/tasks/" + taskId + "/fail?deliveryManId=" + deliveryManId;
        return ResponseEntity.ok(restTemplate.postForObject(url, body, Object.class));
    }

    @PutMapping("/{taskId}/time-window")
    public ResponseEntity<?> changeTimeWindow(@PathVariable String taskId, @RequestParam String deliveryManId, @RequestBody Object body) {
        String url = sessionServiceUrl + "/api/v1/tasks/" + taskId + "/time-window?deliveryManId=" + deliveryManId;
        restTemplate.put(url, body);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/delivery-man/{deliveryManId}/tasks")
    public ResponseEntity<?> getTasksOfDeliveryMan(@PathVariable String deliveryManId,
                                                   @RequestParam(required = false) String beginTime,
                                                   @RequestParam(required = false) String endTime,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(defaultValue = "createdAt") String sortBy,
                                                   @RequestParam(defaultValue = "DESC") String direction) {
        String url = String.format(
            "%s/api/v1/tasks/delivery-man/%s/tasks?beginTime=%s&endTime=%s&page=%d&size=%d&sortBy=%s&direction=%s",
            sessionServiceUrl, deliveryManId, beginTime, endTime, page, size, sortBy, direction);
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    @GetMapping("/delivery-man/{deliveryManId}/tasks/today")
    public ResponseEntity<?> getTasksTodayOfDeliveryMan(@PathVariable String deliveryManId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(defaultValue = "createdAt") String sortBy,
                                                        @RequestParam(defaultValue = "DESC") String direction) {
        String url = String.format(
            "%s/api/v1/tasks/delivery-man/%s/tasks/today?page=%d&size=%d&sortBy=%s&direction=%s",
            sessionServiceUrl, deliveryManId, page, size, sortBy, direction);
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }
}


