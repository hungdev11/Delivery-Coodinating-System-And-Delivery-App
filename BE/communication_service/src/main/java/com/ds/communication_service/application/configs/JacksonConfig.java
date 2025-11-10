package com.ds.communication_service.application.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final String UTC_PLUS_7 = "Asia/Ho_Chi_Minh";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // Configure LocalDateTime to use UTC+7 timezone
        javaTimeModule.addSerializer(
            java.time.LocalDateTime.class,
            new LocalDateTimeSerializer(DATE_TIME_FORMATTER)
        );
        
        ObjectMapper objectMapper = builder
            .modules(javaTimeModule)
            .timeZone(java.util.TimeZone.getTimeZone(UTC_PLUS_7))
            .build();
        
        // Set timezone for serialization/deserialization
        objectMapper.setTimeZone(java.util.TimeZone.getTimeZone(UTC_PLUS_7));
        
        return objectMapper;
    }
}
