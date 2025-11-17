package com.ds.gateway.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Service to initialize Kafka topics when Kafka is ready
 * Creates topics lazily to avoid blocking startup
 */
@Slf4j
@Service
public class TopicInitializationService {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public TopicInitializationService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Initialize topics when application is ready
     * Runs asynchronously to not block startup, but starts immediately
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initializeTopicsOnStartup() {
        // Wait a bit for Kafka to be ready
        try {
            Thread.sleep(3000); // Wait 3 seconds for Kafka to be ready
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        
        // Try to create topics with retries
        int maxRetries = 10; // Increased retries
        int retryDelay = 3000; // 3 seconds initial delay
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                createTopicsIfNotExist();
                log.info("✅ Kafka topics initialized successfully on attempt {}", attempt);
                
                // Wait a bit more to ensure metadata is propagated
                Thread.sleep(2000);
                return; // Success, exit
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    log.warn("⚠️ Attempt {}/{} failed to create topics, retrying in {}ms: {}", 
                        attempt, maxRetries, retryDelay, e.getMessage());
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    // Exponential backoff, but cap at 10 seconds
                    retryDelay = Math.min(retryDelay * 2, 10000);
                } else {
                    log.error("❌ Failed to create topics after {} attempts. Topics may need to be created manually or will be auto-created by Kafka broker when first message is sent", 
                        maxRetries);
                }
            }
        }
    }

    /**
     * Create topics if they don't exist
     * Throws exception if Kafka is not ready (caller will retry)
     */
    public void createTopicsIfNotExist() throws Exception {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // Increased to 30s
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 30000);

        try (AdminClient adminClient = AdminClient.create(configs)) {
            // Check if topics exist with longer timeout
            Set<String> existingTopics = adminClient.listTopics().names().get(30, TimeUnit.SECONDS);
            
            List<NewTopic> topicsToCreate = new ArrayList<>();
            
            if (!existingTopics.contains(KafkaConfig.TOPIC_AUDIT_EVENTS)) {
                topicsToCreate.add(new NewTopic(
                    KafkaConfig.TOPIC_AUDIT_EVENTS,
                    3, // partitions
                    (short) 1 // replicas
                ).configs(Map.of(
                    "retention.ms", "2592000000", // 30 days
                    "cleanup.policy", "delete"
                )));
                log.info("📋 Will create topic: {}", KafkaConfig.TOPIC_AUDIT_EVENTS);
            } else {
                log.debug("✅ Topic already exists: {}", KafkaConfig.TOPIC_AUDIT_EVENTS);
            }
            
            if (!existingTopics.contains(KafkaConfig.TOPIC_AUDIT_EVENTS_DLQ)) {
                topicsToCreate.add(new NewTopic(
                    KafkaConfig.TOPIC_AUDIT_EVENTS_DLQ,
                    1, // partitions
                    (short) 1 // replicas
                ).configs(Map.of(
                    "retention.ms", "7776000000", // 90 days
                    "cleanup.policy", "delete"
                )));
                log.info("📋 Will create topic: {}", KafkaConfig.TOPIC_AUDIT_EVENTS_DLQ);
            } else {
                log.debug("✅ Topic already exists: {}", KafkaConfig.TOPIC_AUDIT_EVENTS_DLQ);
            }

            if (!topicsToCreate.isEmpty()) {
                log.info("🔄 Creating {} Kafka topics...", topicsToCreate.size());
                CreateTopicsResult result = adminClient.createTopics(topicsToCreate);
                result.all().get(30, TimeUnit.SECONDS); // Wait up to 30s for creation
                log.info("✅ Successfully created {} Kafka topics", topicsToCreate.size());
                
                // Verify topics were created by listing again
                Set<String> topicsAfterCreation = adminClient.listTopics().names().get(30, TimeUnit.SECONDS);
                for (NewTopic topic : topicsToCreate) {
                    if (topicsAfterCreation.contains(topic.name())) {
                        log.info("✅ Verified topic exists: {}", topic.name());
                    } else {
                        log.warn("⚠️ Topic creation reported success but topic not found: {}", topic.name());
                    }
                }
                
                // Force refresh producer metadata by accessing partition info
                // This ensures producer knows about the new topics
                try {
                    log.info("🔄 Forcing producer metadata refresh for new topics...");
                    for (NewTopic topic : topicsToCreate) {
                        try {
                            // Access partitions to force metadata refresh
                            kafkaTemplate.partitionsFor(topic.name());
                            log.debug("✅ Metadata refreshed for topic: {}", topic.name());
                        } catch (Exception e) {
                            log.warn("⚠️ Could not refresh metadata for topic {} (will be refreshed automatically): {}", 
                                topic.name(), e.getMessage());
                        }
                    }
                    // Wait a bit for metadata to propagate
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.warn("⚠️ Error refreshing metadata (will be refreshed automatically): {}", e.getMessage());
                }
            } else {
                log.info("✅ All Kafka topics already exist");
            }
            
        } catch (ExecutionException e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                log.info("✅ Topics already exist (created by another instance)");
                return; // Success - topics exist
            }
            log.error("❌ Error creating topics: {}", e.getMessage(), e);
            throw e; // Re-throw to trigger retry
        } catch (Exception e) {
            log.error("❌ Unexpected error creating topics: {}", e.getMessage(), e);
            throw e; // Re-throw to trigger retry
        }
    }
}
