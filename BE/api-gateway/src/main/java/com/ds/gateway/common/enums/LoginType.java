package com.ds.gateway.common.enums;

/**
 * Login type enumeration for simplified system
 * Uses single realm with 2 clients for different user types
 */
public enum LoginType {
    
    /**
     * Backend control login - admin, staff, management
     * Uses 'delivery-system' realm with 'backend-client' client
     */
    BACKEND("delivery-system", "backend-client"),
    
    /**
     * Frontend user login - shipper, client, end users
     * Uses 'delivery-system' realm with 'frontend-client' client
     */
    FRONTEND("delivery-system", "frontend-client");
    
    private final String realm;
    private final String clientId;
    
    LoginType(String realm, String clientId) {
        this.realm = realm;
        this.clientId = clientId;
    }
    
    public String getRealm() {
        return realm;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    /**
     * Get LoginType from string value (case insensitive)
     */
    public static LoginType fromString(String value) {
        if (value == null) {
            return FRONTEND; // Default to FRONTEND
        }
        
        try {
            return LoginType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FRONTEND; // Default to FRONTEND if invalid
        }
    }
}
