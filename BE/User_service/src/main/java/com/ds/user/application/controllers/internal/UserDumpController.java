package com.ds.user.application.controllers.internal;

import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.common.entities.base.User;
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
 * Internal endpoint for dumping all users for snapshot initialization
 * Used by other services to load initial user data into their snapshot tables
 */
@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class UserDumpController {

    private final UserRepository userRepository;

    /**
     * Dump all users for snapshot initialization
     * Returns paginated results to handle large datasets
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 1000, max: 10000)
     * @return List of users with pagination info
     */
    @GetMapping("/user-dump")
    public ResponseEntity<Map<String, Object>> getUserDump(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size) {
        
        // Limit max page size to prevent memory issues
        int pageSize = Math.min(size, 10000);
        
        log.info("📦 User dump requested: page={}, size={}", page, pageSize);
        
        try {
            // Get users using pagination
            Pageable pageable = PageRequest.of(page, pageSize);
            Page<User> userPage = userRepository.findAll(pageable);
            
            // Convert to DTO format for snapshot
            List<Map<String, Object>> users = new ArrayList<>();
            for (User user : userPage.getContent()) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("userId", user.getId());
                userData.put("username", user.getUsername());
                userData.put("email", user.getEmail());
                userData.put("firstName", user.getFirstName());
                userData.put("lastName", user.getLastName());
                userData.put("phone", user.getPhone());
                userData.put("address", user.getAddress());
                userData.put("identityNumber", user.getIdentityNumber());
                userData.put("status", user.getStatus() != null ? user.getStatus().name() : null);
                userData.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
                userData.put("updatedAt", user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null);
                users.add(userData);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("page", userPage.getNumber());
            response.put("size", userPage.getSize());
            response.put("totalElements", userPage.getTotalElements());
            response.put("totalPages", userPage.getTotalPages());
            response.put("hasNext", userPage.hasNext());
            response.put("hasPrevious", userPage.hasPrevious());
            
            log.info("✅ User dump completed: {} users returned (page {}/{})", 
                users.size(), page + 1, userPage.getTotalPages());
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(response);
                    
        } catch (Exception e) {
            log.error("❌ Error generating user dump: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate user dump: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(errorResponse);
        }
    }
}
