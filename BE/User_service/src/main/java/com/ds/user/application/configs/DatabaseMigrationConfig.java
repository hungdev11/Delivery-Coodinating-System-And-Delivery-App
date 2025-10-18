package com.ds.user.application.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Database migration utility for handling schema changes programmatically
 * Similar to Prisma's migration system or Entity Framework migrations
 * 
 * This runs after Hibernate's DDL auto has completed
 */
@Slf4j
@Component
@Profile({"docker", "dev"})  // Only run in development and Docker environments
public class DatabaseMigrationConfig implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🔄 Starting database migration checks...");
        
        try {
            // Check if we need to clean up duplicate usernames
            cleanupDuplicateUsernames();
            
            log.info("✅ Database migration checks completed successfully");
        } catch (Exception e) {
            log.error("❌ Database migration failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Clean up duplicate usernames by appending a suffix to duplicates
     * This is similar to how Prisma handles data migration
     */
    @Transactional
    public void cleanupDuplicateUsernames() {
        log.info("🔍 Checking for duplicate usernames...");
        
        // Find duplicate usernames
        String duplicateQuery = """
            SELECT username, COUNT(*) as count 
            FROM users 
            WHERE username IS NOT NULL 
            GROUP BY username 
            HAVING COUNT(*) > 1
            """;
        
        var duplicates = entityManager.createNativeQuery(duplicateQuery).getResultList();
        
        if (duplicates.isEmpty()) {
            log.info("✅ No duplicate usernames found");
            return;
        }
        
        log.warn("⚠️ Found {} duplicate usernames, cleaning up...", duplicates.size());
        
        // Clean up duplicates by keeping the first occurrence and updating others
        String cleanupQuery = """
            UPDATE users u1
            SET username = CONCAT(username, '_', (
                SELECT COUNT(*) 
                FROM users u2 
                WHERE u2.username = u1.username 
                AND u2.created_at < u1.created_at
            ))
            WHERE u1.username IN (
                SELECT username 
                FROM users 
                WHERE username IS NOT NULL 
                GROUP BY username 
                HAVING COUNT(*) > 1
            )
            AND u1.id NOT IN (
                SELECT MIN(id) 
                FROM users 
                WHERE username IS NOT NULL 
                GROUP BY username 
                HAVING COUNT(*) > 1
            )
            """;
        
        int updatedRows = entityManager.createNativeQuery(cleanupQuery).executeUpdate();
        log.info("✅ Cleaned up {} duplicate usernames", updatedRows);
    }
}
