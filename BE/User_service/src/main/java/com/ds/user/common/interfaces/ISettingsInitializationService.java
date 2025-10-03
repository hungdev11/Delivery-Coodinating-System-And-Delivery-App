package com.ds.user.common.interfaces;

/**
 * Business contract for initializing Keycloak-related settings
 * into the external Settings Service.
 */
public interface ISettingsInitializationService {

    /**
     * Initialize settings if they are not present already.
     * Implementations should be idempotent and safe to call multiple times.
     */
    void initialize();
}
