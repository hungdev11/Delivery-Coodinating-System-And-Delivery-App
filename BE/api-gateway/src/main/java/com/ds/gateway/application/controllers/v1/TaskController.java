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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/session")
@RequiredArgsConstructor
public class TaskController {

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
}


