package com.ds.setting.common.exceptions;

/**
 * Exception thrown when a setting is not found
 */
public class SettingNotFoundException extends RuntimeException {
    public SettingNotFoundException(String message) {
        super(message);
    }
}
