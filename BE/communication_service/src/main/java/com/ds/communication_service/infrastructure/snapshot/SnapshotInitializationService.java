package com.ds.communication_service.infrastructure.snapshot;

import com.ds.communication_service.app_context.models.UserSnapshot;
import com.ds.communication_service.app_context.repositories.UserSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service to initialize UserSnapshot table on startup
 * Downloads full user dump from UserService if snapshot table is empty
 */
@Service
@Slf4j
public class SnapshotInitializationService {

    private final UserSnapshotRepository userSnapshotRepository;
    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://localhost:21501}")
    private String userServiceUrl;

    @Value("${snapshot.initialization.enabled:true}")
    private boolean initializationEnabled;

    public SnapshotInitializationService(UserSnapshotRepository userSnapshotRepository) {
        this.userSnapshotRepository = userSnapshotRepository;
        // Create RestTemplate with timeouts to prevent blocking
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds
        factory.setReadTimeout(10000); // 10 seconds
        this.restTemplate = new RestTemplate(factory);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSnapshotOnStartup() {
        // Run asynchronously to not block application startup
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Wait 2 seconds for services to be ready
                doInitializeSnapshot();
            } catch (Exception e) {
                log.error("❌ Error in async snapshot initialization: {}", e.getMessage(), e);
            }
        }, "snapshot-init-thread").start();
    }

    /**
     * Initialize snapshot - can be called from startup or from Kafka event
     * This method should not block - it's called asynchronously
     */
    @Transactional
    public void doInitializeSnapshot() {
        if (!initializationEnabled) {
            log.debug("[communication-service] [SnapshotInitializationService.doInitializeSnapshot] Snapshot initialization disabled, skipping...");
            return;
        }

        try {
            // Check if snapshot table is empty
            long snapshotCount = userSnapshotRepository.count();
            
            if (snapshotCount > 0) {
                log.debug("[communication-service] [SnapshotInitializationService.doInitializeSnapshot] UserSnapshot table already has {} records, skipping initialization", snapshotCount);
                return;
            }

            log.debug("[communication-service] [SnapshotInitializationService.doInitializeSnapshot] UserSnapshot table is empty, starting initialization...");
            
            // Download and load all users
            int totalLoaded = loadAllUsersFromDump();
            
            log.debug("[communication-service] [SnapshotInitializationService.doInitializeSnapshot] Snapshot initialization completed: {} users loaded", totalLoaded);
            
        } catch (Exception e) {
            log.error("[communication-service] [SnapshotInitializationService.doInitializeSnapshot] Error during snapshot initialization", e);
            // Don't throw - allow application to start even if initialization fails
            // Kafka consumer will handle updates going forward
        }
    }

    private int loadAllUsersFromDump() {
        int totalLoaded = 0;
        int page = 0;
        int pageSize = 1000;
        boolean hasMore = true;

        while (hasMore) {
            try {
                String url = String.format("%s/internal/user-dump?page=%d&size=%d", 
                    userServiceUrl, page, pageSize);
                
                log.debug("[communication-service] [SnapshotInitializationService.loadAllUsersFromDump] Downloading user dump: page={}, size={}", page, pageSize);
                
                ParameterizedTypeReference<Map<String, Object>> responseType = 
                    new ParameterizedTypeReference<Map<String, Object>>() {};
                
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, responseType);
                
                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    log.error("[communication-service] [SnapshotInitializationService.loadAllUsersFromDump] Failed to download user dump: status={}", response.getStatusCode());
                    break;
                }

                Map<String, Object> body = response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> users = (List<Map<String, Object>>) body.get("users");
                
                if (users == null || users.isEmpty()) {
                    log.debug("[communication-service] [SnapshotInitializationService.loadAllUsersFromDump] No users returned in dump response");
                    break;
                }

                // Bulk insert users into snapshot table
                List<UserSnapshot> snapshots = new ArrayList<>();
                for (Map<String, Object> userData : users) {
                    UserSnapshot snapshot = UserSnapshot.builder()
                        .userId((String) userData.get("userId"))
                        .username((String) userData.get("username"))
                        .firstName((String) userData.get("firstName"))
                        .lastName((String) userData.get("lastName"))
                        .email((String) userData.get("email"))
                        .phone((String) userData.get("phone"))
                        .address((String) userData.get("address"))
                        .identityNumber((String) userData.get("identityNumber"))
                        .status((String) userData.get("status"))
                        .createdAt(parseDateTime(userData.get("createdAt")))
                        .updatedAt(parseDateTime(userData.get("updatedAt")))
                        .build();
                    snapshots.add(snapshot);
                }

                // Batch save
                userSnapshotRepository.saveAll(snapshots);
                totalLoaded += snapshots.size();
                
                log.debug("[communication-service] [SnapshotInitializationService.loadAllUsersFromDump] Loaded {} users (total: {})", snapshots.size(), totalLoaded);

                // Check if there are more pages
                Boolean hasNext = (Boolean) body.get("hasNext");
                hasMore = Boolean.TRUE.equals(hasNext);
                page++;

            } catch (Exception e) {
                log.error("[communication-service] [SnapshotInitializationService.loadAllUsersFromDump] Error loading users from dump (page {})", page, e);
                break;
            }
        }

        return totalLoaded;
    }

    private LocalDateTime parseDateTime(Object dateTimeObj) {
        if (dateTimeObj == null) {
            return LocalDateTime.now();
        }
        try {
            String dateTimeStr = dateTimeObj.toString();
            // Handle ISO format: "2024-01-01T12:00:00" or "2024-01-01T12:00:00Z"
            dateTimeStr = dateTimeStr.replace("Z", "").replace("T", "T");
            if (dateTimeStr.length() > 19) {
                dateTimeStr = dateTimeStr.substring(0, 19);
            }
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            log.debug("[communication-service] [SnapshotInitializationService.parseDateTime] Failed to parse datetime: {}, using now()", dateTimeObj);
            return LocalDateTime.now();
        }
    }
}
