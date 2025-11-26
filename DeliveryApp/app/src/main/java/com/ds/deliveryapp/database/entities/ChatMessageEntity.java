package com.ds.deliveryapp.database.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Room entity for storing chat messages locally
 */
@Entity(tableName = "chat_messages")
public class ChatMessageEntity {
    
    @PrimaryKey
    @NonNull
    private String id;
    
    private String conversationId;
    private String senderId;
    private String content;
    private String sentAt;
    private String type; // TEXT, INTERACTIVE_PROPOSAL
    private String status; // SENT, DELIVERED, READ
    private String deliveredAt;
    private String readAt;
    private String proposalJson; // Serialized proposal data
    
    // Constructors
    public ChatMessageEntity() {}
    
    @Ignore
    public ChatMessageEntity(@NonNull String id, String conversationId, String senderId, 
                            String content, String sentAt, String type, String status,
                            String deliveredAt, String readAt, String proposalJson) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.sentAt = sentAt;
        this.type = type;
        this.status = status;
        this.deliveredAt = deliveredAt;
        this.readAt = readAt;
        this.proposalJson = proposalJson;
    }
    
    // Getters and Setters
    @NonNull
    public String getId() {
        return id;
    }
    
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getDeliveredAt() {
        return deliveredAt;
    }
    
    public void setDeliveredAt(String deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
    
    public String getReadAt() {
        return readAt;
    }
    
    public void setReadAt(String readAt) {
        this.readAt = readAt;
    }
    
    public String getProposalJson() {
        return proposalJson;
    }
    
    public void setProposalJson(String proposalJson) {
        this.proposalJson = proposalJson;
    }
}
