package ptithcm.graduation.apigateway.services.baseService.product.dto;

public class CreateProductRequestDto {
    private String storeId;
    private String staffId;
    private String name;
    private String imgLink;

    // Default constructor
    public CreateProductRequestDto() {}

    // All args constructor
    public CreateProductRequestDto(String storeId, String staffId, String name, String imgLink) {
        this.storeId = storeId;
        this.staffId = staffId;
        this.name = name;
        this.imgLink = imgLink;
    }

    // Getters and Setters
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImgLink() { return imgLink; }
    public void setImgLink(String imgLink) { this.imgLink = imgLink; }
}
