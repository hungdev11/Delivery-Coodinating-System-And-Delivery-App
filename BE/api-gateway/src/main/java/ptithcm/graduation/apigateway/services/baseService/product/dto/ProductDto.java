package ptithcm.graduation.apigateway.services.baseService.product.dto;

import java.time.Instant;

public class ProductDto {
    private String id;
    private String storeId;
    private String staffId;
    private String title;
    private String content;
    private String description;
    private String imgLink;
    private Long totalWaitingShip;
    private Long totalHearts;
    private Long totalViews;
    private String langCode;
    private Boolean enable;
    private Instant createdAt;
    private Instant updatedAt;

    // Default constructor
    public ProductDto() {}

    // All args constructor
    public ProductDto(String id, String storeId, String staffId, String title, 
                     String content, String description, String imgLink, 
                     Long totalWaitingShip, Long totalHearts, Long totalViews, 
                     String langCode, Boolean enable, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.staffId = staffId;
        this.title = title;
        this.content = content;
        this.description = description;
        this.imgLink = imgLink;
        this.totalWaitingShip = totalWaitingShip;
        this.totalHearts = totalHearts;
        this.totalViews = totalViews;
        this.langCode = langCode;
        this.enable = enable;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImgLink() { return imgLink; }
    public void setImgLink(String imgLink) { this.imgLink = imgLink; }

    public Long getTotalWaitingShip() { return totalWaitingShip; }
    public void setTotalWaitingShip(Long totalWaitingShip) { this.totalWaitingShip = totalWaitingShip; }

    public Long getTotalHearts() { return totalHearts; }
    public void setTotalHearts(Long totalHearts) { this.totalHearts = totalHearts; }

    public Long getTotalViews() { return totalViews; }
    public void setTotalViews(Long totalViews) { this.totalViews = totalViews; }

    public String getLangCode() { return langCode; }
    public void setLangCode(String langCode) { this.langCode = langCode; }

    public Boolean getEnable() { return enable; }
    public void setEnable(Boolean enable) { this.enable = enable; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
