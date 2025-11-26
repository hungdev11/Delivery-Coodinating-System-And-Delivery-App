package com.ds.session.session_service.application.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

/**
 * Transaction configuration to resolve the ambiguity between JPA and Kafka transaction managers.
 * 
 * When Kafka transactions are enabled (via transaction-id-prefix), Spring Boot auto-configures
 * a KafkaTransactionManager. This conflicts with the auto-configured JPA transaction manager.
 * 
 * This configuration makes the JPA transaction manager primary so @Transactional annotations
 * use it by default for database operations.
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    /**
     * Primary transaction manager for JPA/database operations.
     * This will be used by default when @Transactional is used without specifying a transaction manager.
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
