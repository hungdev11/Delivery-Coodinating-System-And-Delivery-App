package com.ds.project.app_context.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * Setting entity for application configuration
 */
@Entity
@Table(name = "settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Setting {
    
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private String id;
    
    @Column(name = "`key`", nullable = false)
    private String key;
    
    @Column(name = "`group`", nullable = false)
    private String group;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettingType type;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettingLevel level;
    
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
    
    public enum SettingType {
        STRING, INTEGER, BOOLEAN, JSON, ARRAY
    }
    
    public enum SettingLevel {
        SYSTEM, USER, ORGANIZATION
    }
}
