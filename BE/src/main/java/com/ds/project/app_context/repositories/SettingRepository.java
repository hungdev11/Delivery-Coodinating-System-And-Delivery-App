package com.ds.project.app_context.repositories;

import com.ds.project.app_context.models.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Setting entity
 */
@Repository
public interface SettingRepository extends JpaRepository<Setting, String> {
    
    /**
     * Find setting by key
     */
    Optional<Setting> findByKey(String key);
    
    /**
     * Find settings by group
     */
    List<Setting> findByGroup(String group);
    
    /**
     * Check if setting exists by key
     */
    boolean existsByKey(String key);
    
    /**
     * Find settings by level
     */
    List<Setting> findByLevel(String level);
    
    /**
     * Find settings by type
     */
    List<Setting> findByType(String type);
}
