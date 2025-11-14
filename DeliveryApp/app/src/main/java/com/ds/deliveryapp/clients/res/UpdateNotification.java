package com.ds.deliveryapp.clients.res;

/**
 * DTO for update notifications from Communication service
 * Sent via WebSocket to notify clients when data needs to be refreshed
 * Matches UpdateNotificationDTO from backend
 */
public class UpdateNotification {
    private UpdateType updateType;
    private EntityType entityType;
    private String entityId;
    private ActionType action;
    private String userId;
    private ClientType clientType;
    private String payload; // Optional JSON payload with detailed data
    
    public UpdateType getUpdateType() {
        return updateType;
    }
    
    public void setUpdateType(UpdateType updateType) {
        this.updateType = updateType;
    }
    
    public EntityType getEntityType() {
        return entityType;
    }
    
    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
    
    public ActionType getAction() {
        return action;
    }
    
    public void setAction(ActionType action) {
        this.action = action;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public ClientType getClientType() {
        return clientType;
    }
    
    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public void setPayload(String payload) {
        this.payload = payload;
    }
    
    public enum UpdateType {
        ENTITY_UPDATE,
        STATUS_CHANGED,
        ASSIGNMENT_UPDATE,
        SESSION_UPDATE,
        PARCEL_UPDATE,
        USER_UPDATE,
        ROUTE_UPDATE
    }
    
    public enum EntityType {
        PARCEL,
        SESSION,
        ASSIGNMENT,
        USER,
        CONVERSATION,
        MESSAGE
    }
    
    public enum ActionType {
        CREATED,
        UPDATED,
        DELETED,
        STATUS_CHANGED,
        ASSIGNED,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    public enum ClientType {
        ANDROID,
        WEB,
        ALL
    }
}
