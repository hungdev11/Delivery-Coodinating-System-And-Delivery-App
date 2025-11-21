package com.ds.session.session_service.infrastructure.kafka;

import java.util.HashMap;
import java.util.Map;

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
import com.ds.session.session_service.infrastructure.kafka.dto.UserEventDto;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:session-service-group}")
    private String groupId;

    // Topics used by session-service to request parcel transitions
    public static final String TOPIC_PARCEL_STATUS_REQUEST = "parcel-status-request";
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
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // Create JsonDeserializer that ignores type info from producer and uses local DTO
        JsonDeserializer<UserEventDto> jsonDeserializer = new JsonDeserializer<>(UserEventDto.class);
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.setRemoveTypeHeaders(true);
        jsonDeserializer.addTrustedPackages("com.ds.session.session_service.common.entities.dto", 
                "com.ds.session.session_service.business.v1.services", 
                "com.ds.session.session_service.infrastructure.kafka.dto");
        
        // Wrap with ErrorHandlingDeserializer
        ErrorHandlingDeserializer<UserEventDto> errorHandlingDeserializer = 
                new ErrorHandlingDeserializer<>(jsonDeserializer);
        
        // Disable auto-commit and use manual ack so consumer offset commits can be controlled
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // Use read_committed isolation to avoid reading uncommitted transactional messages
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        // Create factory and set deserializers
        DefaultKafkaConsumerFactory<String, Object> factory = new DefaultKafkaConsumerFactory<>(config);
        factory.setKeyDeserializer(new StringDeserializer());
        @SuppressWarnings("unchecked")
        org.apache.kafka.common.serialization.Deserializer<Object> valueDeserializer = 
                (org.apache.kafka.common.serialization.Deserializer<Object>) (org.apache.kafka.common.serialization.Deserializer<?>) errorHandlingDeserializer;
        factory.setValueDeserializer(valueDeserializer);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
    // Manual ack so we control committing offsets after successful processing and to integrate with Kafka transactions
    factory.getContainerProperties().setAckMode(
            org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    factory.getContainerProperties().setSyncCommits(true);
        return factory;
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
    // parcel-status-changed topic removed (not used currently)
}
