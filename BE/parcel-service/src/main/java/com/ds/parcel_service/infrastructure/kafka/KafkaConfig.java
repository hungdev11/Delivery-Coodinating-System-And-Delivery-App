package com.ds.parcel_service.infrastructure.kafka;

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
import com.ds.parcel_service.infrastructure.kafka.dto.UserEventDto;
import com.ds.parcel_service.common.events.ParcelStatusRequestEvent;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:parcel-service-group}")
    private String groupId;

    public static final String TOPIC_PARCEL_STATUS_REQUEST = "parcel-status-request";
    public static final String TOPIC_AUDIT_EVENTS = "audit-events";
    public static final String TOPIC_AUDIT_EVENTS_DLQ = "audit-events-dlq";
    // TOPIC_PARCEL_STATUS_CHANGED removed (not used currently)

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        // enable idempotence for safer producer
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // NO transaction-id-prefix - keep it simple like user-service
        return new DefaultKafkaProducerFactory<>(config);
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
        jsonDeserializer.addTrustedPackages("com.ds.parcel_service.common.entities.dto", 
                "com.ds.parcel_service.business.v1.services", 
                "com.ds.parcel_service.infrastructure.kafka.dto");
        
        // Wrap with ErrorHandlingDeserializer
        ErrorHandlingDeserializer<UserEventDto> errorHandlingDeserializer = 
                new ErrorHandlingDeserializer<>(jsonDeserializer);
        
        // For exactly-once processing, disable auto commit and set isolation level to read_committed
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
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
    public ConsumerFactory<String, Object> parcelStatusRequestConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // Create JsonDeserializer for ParcelStatusRequestEvent
        JsonDeserializer<ParcelStatusRequestEvent> jsonDeserializer = new JsonDeserializer<>(ParcelStatusRequestEvent.class);
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.setRemoveTypeHeaders(true);
        jsonDeserializer.addTrustedPackages("com.ds.parcel_service.common.events", 
                "com.ds.parcel_service.common.entities.dto", 
                "com.ds.parcel_service.business.v1.services", 
                "com.ds.parcel_service.infrastructure.kafka.dto");
        
        // Wrap with ErrorHandlingDeserializer
        ErrorHandlingDeserializer<ParcelStatusRequestEvent> errorHandlingDeserializer = 
                new ErrorHandlingDeserializer<>(jsonDeserializer);
        
        // For exactly-once processing, disable auto commit and set isolation level to read_committed
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
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
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        // Manual ack so we can control committing offsets after successful processing and (optionally) transactions
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        // ensure listener runs in a Kafka transaction if needed (producer will be transactional)
        factory.getContainerProperties().setSyncCommits(true);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> parcelStatusRequestListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(parcelStatusRequestConsumerFactory());
        factory.setConcurrency(3);
        // Manual ack so we can control committing offsets after successful processing and (optionally) transactions
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        // ensure listener runs in a Kafka transaction if needed (producer will be transactional)
        factory.getContainerProperties().setSyncCommits(true);
        return factory;
    }

    @Bean
    public NewTopic parcelStatusRequestTopic() {
        return TopicBuilder.name(TOPIC_PARCEL_STATUS_REQUEST)
                .partitions(6)
                .replicas(1)
                .config("retention.ms", "259200000")
                .build();
    }

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(TOPIC_AUDIT_EVENTS)
                .partitions(3) // Partition by userId or resourceType
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days
                .config("cleanup.policy", "delete")
                .build();
    }

    @Bean
    public NewTopic auditEventsDlqTopic() {
        return TopicBuilder.name(TOPIC_AUDIT_EVENTS_DLQ)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "7776000000") // 90 days (longer retention for failed events)
                .config("cleanup.policy", "delete")
                .build();
    }
}
