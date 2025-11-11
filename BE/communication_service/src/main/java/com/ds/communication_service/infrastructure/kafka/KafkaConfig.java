package com.ds.communication_service.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for message queuing and event streaming
 * Topics:
 * - chat-messages: Message queuing for guaranteed delivery
 * - message-status-events: Status updates (SENT→DELIVERED→READ)
 * - typing-events: Typing indicator events
 * - notifications: In-app notification events
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:communication-service-group}")
    private String groupId;

    // Topic names
    public static final String TOPIC_CHAT_MESSAGES = "chat-messages";
    public static final String TOPIC_MESSAGE_STATUS = "message-status-events";
    public static final String TOPIC_TYPING_EVENTS = "typing-events";
    public static final String TOPIC_NOTIFICATIONS = "notifications";

    /**
     * Producer configuration
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Producer reliability settings
        config.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Prevent duplicate messages
        
        // Performance settings
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        // Use gzip compression instead of snappy (snappy requires native libraries not available in Docker)
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Consumer configuration
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ds.communication_service.common.dto");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);
        
        // Consumer reliability settings
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit for reliability
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // Number of consumer threads
        factory.getContainerProperties().setAckMode(
            org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    /**
     * Topic creation beans
     * Topics are created automatically if they don't exist
     */
    @Bean
    public NewTopic chatMessagesTopic() {
        return TopicBuilder.name(TOPIC_CHAT_MESSAGES)
                .partitions(3) // Partition by conversationId for ordering
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .config("cleanup.policy", "delete")
                .build();
    }

    @Bean
    public NewTopic messageStatusTopic() {
        return TopicBuilder.name(TOPIC_MESSAGE_STATUS)
                .partitions(3) // Partition by userId
                .replicas(1)
                .config("retention.ms", "86400000") // 1 day
                .config("cleanup.policy", "delete")
                .build();
    }

    @Bean
    public NewTopic typingEventsTopic() {
        return TopicBuilder.name(TOPIC_TYPING_EVENTS)
                .partitions(3) // Partition by conversationId
                .replicas(1)
                .config("retention.ms", "60000") // 1 minute (short retention for typing events)
                .config("cleanup.policy", "delete")
                .build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(TOPIC_NOTIFICATIONS)
                .partitions(3) // Partition by userId
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days
                .config("cleanup.policy", "delete")
                .build();
    }
}
