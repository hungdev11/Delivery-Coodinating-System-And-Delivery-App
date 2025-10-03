package com.ds.setting.common.exceptions;

/**
 * Exception thrown when attempting to modify a read-only setting
 */
public class ReadOnlySettingException extends RuntimeException {
    public ReadOnlySettingException(String message) {
        super(message);
    }
}

