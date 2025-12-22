package com.ds.deliveryapp.model;

import java.io.Serializable;

public class DeliveryProof implements Serializable {
    private String id;
    private String type; // DELIVERED, RETURNED
    private String mediaUrl;
    private String confirmedBy;
    private String createdAt;

    public DeliveryProof() {
    }

    public DeliveryProof(String id, String type, String mediaUrl, String confirmedBy, String createdAt) {
        this.id = id;
        this.type = type;
        this.mediaUrl = mediaUrl;
        this.confirmedBy = confirmedBy;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isVideo() {
        if (mediaUrl == null) return false;
        String lower = mediaUrl.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || 
               lower.endsWith(".avi") || lower.contains("/video/");
    }
}
