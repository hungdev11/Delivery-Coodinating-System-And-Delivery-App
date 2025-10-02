package com.ds.user.common.interfaces;

public interface ISettingsWriterService {
    void createSetting(String key, String group, String description,
                       String type, String value, String level, boolean isReadOnly, String displayMode);
}
