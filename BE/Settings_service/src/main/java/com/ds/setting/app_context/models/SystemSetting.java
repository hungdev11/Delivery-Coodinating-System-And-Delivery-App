package com.ds.setting.app_context.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * System Setting Entity
 * Stores application configuration and secrets
 */
@Entity
@Table(name = "system_settings", indexes = {
    @Index(name = "idx_setting_group", columnList = "setting_group"),
    @Index(name = "idx_setting_level", columnList = "level")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @Column(name = "setting_key", length = 100, nullable = false)
    private String key;

    @Column(name = "setting_group", length = 50, nullable = false)
    private String group;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", length = 20, nullable = false)
    private SettingType type;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String value;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "level", nullable = false)
    private SettingLevel level;

    @Column(name = "is_read_only", nullable = false)
    private Boolean isReadOnly;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_mode", length = 20, nullable = false)
    @Builder.Default
    private DisplayMode displayMode = DisplayMode.TEXT;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Setting Type Enum
     */
    public enum SettingType {
        STRING,
        INTEGER,
        DECIMAL,
        BOOLEAN,
        JSON
    }

    /**
     * Setting Level Enum
     * Định nghĩa mức độ quan trọng/phạm vi của setting
     * Sử dụng ORDINAL (số) để lưu trong database
     */
    public enum SettingLevel {
        SYSTEM(0),      // System-wide settings (highest priority)
        APPLICATION(1), // Application-level settings
        SERVICE(2),     // Service-specific settings
        FEATURE(3),     // Feature-specific settings
        USER(4);        // User-level settings (lowest priority)
        
        private final int value;
        
        SettingLevel(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }

    /**
     * Display Mode Enum
     * Định nghĩa cách hiển thị setting trong UI
     */
    public enum DisplayMode {
        TEXT,        // Plain text
        PASSWORD,    // Masked password (****)
        CODE,        // Code editor (JSON, XML, etc.)
        NUMBER,      // Number input
        TOGGLE,      // Boolean toggle
        TEXTAREA,    // Multi-line text
        URL,         // URL/Link
        EMAIL        // Email address
    }
}
