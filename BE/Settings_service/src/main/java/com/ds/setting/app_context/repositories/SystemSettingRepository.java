package com.ds.setting.app_context.repositories;

import com.ds.setting.app_context.models.SystemSetting;
import com.ds.setting.app_context.models.SystemSetting.SettingLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SystemSetting entity
 */
@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {

    /**
     * Find settings by group
     */
    List<SystemSetting> findByGroup(String group);

    /**
     * Find settings by level
     */
    List<SystemSetting> findByLevel(SettingLevel level);

    /**
     * Find read-only settings
     */
    List<SystemSetting> findByIsReadOnlyTrue();

    /**
     * Find settings by group and level
     */
    List<SystemSetting> findByGroupAndLevel(String group, SettingLevel level);

    /**
     * Check if setting exists and is read-only
     */
    @Query("SELECT s.isReadOnly FROM SystemSetting s WHERE s.key = :key")
    Optional<Boolean> isReadOnly(@Param("key") String key);

    /**
     * Find all non-read-only settings
     */
    List<SystemSetting> findByIsReadOnlyFalse();

    /**
     * Search settings by key or description
     */
    @Query("SELECT s FROM SystemSetting s WHERE " +
           "LOWER(s.key) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<SystemSetting> searchSettings(@Param("search") String search);

    /**
     * Find setting by key and group (composite lookup)
     */
    Optional<SystemSetting> findByKeyAndGroup(String key, String group);
}
