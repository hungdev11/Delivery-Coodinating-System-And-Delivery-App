package ptithcm.graduation.apigateway.services.baseService.product.dto;

public class UpdateProductRequestDto {
    private String id;
    private String storeId;
    private String staffId;
    private String title;
    private String content;
    private String description;
    private String imgLink;
    private Integer totalWaitingShip;
    private Integer totalHearts;
    private Integer totalViews;
    private String langCode;
    private Boolean enable;

    // Default constructor
    public UpdateProductRequestDto() {}

    // All args constructor
    public UpdateProductRequestDto(String id, String storeId, String staffId, String title, 
                                  String content, String description, String imgLink, 
                                  Integer totalWaitingShip, Integer totalHearts, Integer totalViews, 
                                  String langCode, Boolean enable) {
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

    public Integer getTotalWaitingShip() { return totalWaitingShip; }
    public void setTotalWaitingShip(Integer totalWaitingShip) { this.totalWaitingShip = totalWaitingShip; }

    public Integer getTotalHearts() { return totalHearts; }
    public void setTotalHearts(Integer totalHearts) { this.totalHearts = totalHearts; }

    public Integer getTotalViews() { return totalViews; }
    public void setTotalViews(Integer totalViews) { this.totalViews = totalViews; }

    public String getLangCode() { return langCode; }
    public void setLangCode(String langCode) { this.langCode = langCode; }

    public Boolean getEnable() { return enable; }
    public void setEnable(Boolean enable) { this.enable = enable; }
}
