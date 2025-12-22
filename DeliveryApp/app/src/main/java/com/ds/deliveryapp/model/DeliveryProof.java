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

    /**
     * Get thumbnail URL for video from Cloudinary
     * Cloudinary video URLs can be converted to thumbnail by:
     * - Replacing /video/upload/ with /video/upload/so_0,w_300,h_300,c_fill/
     * - Changing extension from .mp4/.mov to .jpg
     */
    public String getThumbnailUrl() {
        if (!isVideo() || mediaUrl == null) {
            return mediaUrl; // Return original URL for images
        }
        
        // Cloudinary video URL format: https://res.cloudinary.com/.../video/upload/v1234567890/filename.mp4
        // Thumbnail format: https://res.cloudinary.com/.../video/upload/so_0,w_300,h_300,c_fill/v1234567890/filename.jpg
        
        try {
            // Check if it's a Cloudinary URL
            if (mediaUrl.contains("cloudinary.com") && mediaUrl.contains("/video/upload/")) {
                // Extract the path after /video/upload/
                int uploadIndex = mediaUrl.indexOf("/video/upload/");
                if (uploadIndex != -1) {
                    String baseUrl = mediaUrl.substring(0, uploadIndex + "/video/upload/".length());
                    String restOfUrl = mediaUrl.substring(uploadIndex + "/video/upload/".length());
                    
                    // Add thumbnail transformation: so_0 (start at 0s), w_300, h_300, c_fill
                    String thumbnailUrl = baseUrl + "so_0,w_300,h_300,c_fill/" + restOfUrl;
                    
                    // Change extension to .jpg
                    thumbnailUrl = thumbnailUrl.replaceAll("\\.(mp4|mov|avi)$", ".jpg");
                    
                    return thumbnailUrl;
                }
            }
        } catch (Exception e) {
            // If transformation fails, return original URL
        }
        
        return mediaUrl;
    }
}
