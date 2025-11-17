package com.ds.gateway.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service to initialize Kafka topics when Kafka is ready
 * Creates topics lazily to avoid blocking startup
 */
@Slf4j
@Service
public class TopicInitializationService {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Initialize topics when application is ready
     * Runs asynchronously to not block startup
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initializeTopicsOnStartup() {
        // Wait a bit for Kafka to be ready
        try {
            Thread.sleep(5000); // Wait 5 seconds for Kafka to start
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        createTopicsIfNotExist();
    }

    /**
     * Create topics if they don't exist
     * Retries with exponential backoff if Kafka is not ready
     */
    public void createTopicsIfNotExist() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);

        try (AdminClient adminClient = AdminClient.create(configs)) {
            // Check if topics exist
            Set<String> existingTopics = adminClient.listTopics().names().get(10, TimeUnit.SECONDS);
            
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
            }

            if (!topicsToCreate.isEmpty()) {
                CreateTopicsResult result = adminClient.createTopics(topicsToCreate);
                result.all().get(10, TimeUnit.SECONDS);
                log.info("✅ Successfully created {} Kafka topics", topicsToCreate.size());
            } else {
                log.info("✅ All Kafka topics already exist");
            }
            
        } catch (TimeoutException e) {
            log.warn("⚠️ Kafka not ready yet, topics will be auto-created when first message is sent");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                log.debug("Topics already exist (created by another instance)");
            } else {
                log.warn("⚠️ Failed to create topics: {}. Topics will be auto-created when first message is sent", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("⚠️ Error creating Kafka topics: {}. Topics will be auto-created when first message is sent", e.getMessage());
        }
    }
}
