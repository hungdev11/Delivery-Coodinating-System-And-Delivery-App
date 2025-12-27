package com.ds.session.session_service.infrastructure.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // Topics used by session-service to request parcel transitions
    public static final String TOPIC_PARCEL_STATUS_REQUEST = "parcel-status-request";
    // Topic for assignment completed events (consumed by communication-service)
    public static final String TOPIC_ASSIGNMENT_COMPLETED = "assignment-completed";
    // Topic for parcel postponed events (consumed by communication-service)
    public static final String TOPIC_PARCEL_POSTPONED = "parcel-postponed";
    // Topic for session completed events (consumed by communication-service)
    public static final String TOPIC_SESSION_COMPLETED = "session-completed";
    // TOPIC_PARCEL_STATUS_CHANGED constant removed (not used currently)

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");

        DefaultKafkaProducerFactory<String, Object> pf = new DefaultKafkaProducerFactory<>(config);
        // enable transactional producer for exactly-once semantics
        pf.setTransactionIdPrefix("session-tx-");
        return pf;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }


    @Bean
    public NewTopic parcelStatusRequestTopic() {
        return TopicBuilder.name(TOPIC_PARCEL_STATUS_REQUEST)
                .partitions(6) // partition by parcelId for ordering
                .replicas(1)
                .config("retention.ms", "259200000") // 3 days
                .config("cleanup.policy", "delete")
                .build();
    }
    
    @Bean
    public NewTopic assignmentCompletedTopic() {
        return TopicBuilder.name(TOPIC_ASSIGNMENT_COMPLETED)
                .partitions(3) // partition by parcelId for ordering
                .replicas(1)
                .config("retention.ms", "259200000") // 3 days
                .config("cleanup.policy", "delete")
                .build();
    }
    
    @Bean
    public NewTopic parcelPostponedTopic() {
        return TopicBuilder.name(TOPIC_PARCEL_POSTPONED)
                .partitions(3) // partition by parcelId for ordering
                .replicas(1)
                .config("retention.ms", "259200000") // 3 days
                .config("cleanup.policy", "delete")
                .build();
    }
    
    @Bean
    public NewTopic sessionCompletedTopic() {
        return TopicBuilder.name(TOPIC_SESSION_COMPLETED)
                .partitions(3) // partition by sessionId for ordering
                .replicas(1)
                .config("retention.ms", "259200000") // 3 days
                .config("cleanup.policy", "delete")
                .build();
    }
    // parcel-status-changed topic removed (not used currently)
}
