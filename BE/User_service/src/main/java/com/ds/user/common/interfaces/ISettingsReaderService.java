package com.ds.user.common.interfaces;

import java.util.Optional;

/**
 * Business contract for reading settings from the Settings Service
 */
public interface ISettingsReaderService {

    /**
     * Get setting value by key and group
     */
    Optional<String> getSettingValue(String key, String group);

    /**
     * Get setting value by key and group with default value
     */
    String getSettingValue(String key, String group, String defaultValue);

    /**
     * Check if a setting exists by key and group
     */
    boolean settingExists(String key, String group);
}
