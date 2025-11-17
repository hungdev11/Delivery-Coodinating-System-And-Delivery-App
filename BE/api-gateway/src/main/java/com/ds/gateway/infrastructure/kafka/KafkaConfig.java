package com.ds.gateway.infrastructure.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for API Gateway
 * Publishes audit events for all operations
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // Topic names
    public static final String TOPIC_AUDIT_EVENTS = "audit-events";
    public static final String TOPIC_AUDIT_EVENTS_DLQ = "audit-events-dlq"; // Dead Letter Queue

    /**
     * KafkaAdmin bean for topic management
     * Configured to not block startup if Kafka is unavailable
     * Topics will be created by TopicInitializationService after startup
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Increase timeout to avoid quick failures, but still fail fast if Kafka is truly unavailable
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000); // 10s timeout
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 10000);
        // Connection timeout - fail fast if can't connect
        configs.put(AdminClientConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 10000);
        
        KafkaAdmin admin = new KafkaAdmin(configs);
        // Disable auto-create - topics will be created by TopicInitializationService
        // This prevents KafkaAdmin from trying to create topics during startup
        admin.setAutoCreate(false);
        // Don't fail startup if broker unavailable - topics will be created when Kafka is ready
        admin.setFatalIfBrokerNotAvailable(false);
        return admin;
    }

    /**
     * Producer configuration
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Create ObjectMapper with JavaTimeModule for LocalDateTime support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Create JsonSerializer without type information
        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(objectMapper);
        jsonSerializer.setAddTypeInfo(false);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, jsonSerializer.getClass());
        
        // Producer reliability settings
        config.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Prevent duplicate messages
        
        // Increase metadata timeout to allow topics to be created
        // Topics will be auto-created by Kafka broker or TopicInitializationService
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000); // 60s timeout to allow topic creation and metadata refresh
        config.put(ProducerConfig.METADATA_MAX_AGE_CONFIG, 60000); // 1 minute - refresh metadata more frequently
        config.put(ProducerConfig.METADATA_MAX_IDLE_CONFIG, 60000); // 1 minute
        
        // Performance settings
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        
        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(config);
        factory.setValueSerializer(jsonSerializer);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Audit events topic
     * High retention for audit trail
     * Note: Topics are created by TopicInitializationService after startup to avoid blocking
     * These beans are kept for reference but won't be used during startup
     */
    // @Bean - Commented out to prevent KafkaAdmin from trying to create topics during startup
    // Topics will be created by TopicInitializationService instead
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(TOPIC_AUDIT_EVENTS)
                .partitions(3) // Partition by userId or resourceType
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Dead Letter Queue for failed audit events
     * Events that fail to be processed will be sent here for manual review
     * Note: Topics are created by TopicInitializationService after startup to avoid blocking
     * These beans are kept for reference but won't be used during startup
     */
    // @Bean - Commented out to prevent KafkaAdmin from trying to create topics during startup
    // Topics will be created by TopicInitializationService instead
    public NewTopic auditEventsDlqTopic() {
        return TopicBuilder.name(TOPIC_AUDIT_EVENTS_DLQ)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "7776000000") // 90 days (longer retention for failed events)
                .config("cleanup.policy", "delete")
                .build();
    }
}
