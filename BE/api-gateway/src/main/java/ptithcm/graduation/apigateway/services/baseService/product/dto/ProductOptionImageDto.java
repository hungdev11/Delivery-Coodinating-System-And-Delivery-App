package ptithcm.graduation.apigateway.services.baseService.product.dto;

public class ProductOptionImageDto {
    private Long id;
    private Long productOptionId;
    private ProductOptionDto productOption;
    private String title;
    private String imgUrl;
    private Boolean enable;

    // Default constructor
    public ProductOptionImageDto() {}

    // All args constructor
    public ProductOptionImageDto(Long id, Long productOptionId, ProductOptionDto productOption, 
                                String title, String imgUrl, Boolean enable) {
        this.id = id;
        this.productOptionId = productOptionId;
        this.productOption = productOption;
        this.title = title;
        this.imgUrl = imgUrl;
        this.enable = enable;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductOptionId() { return productOptionId; }
    public void setProductOptionId(Long productOptionId) { this.productOptionId = productOptionId; }

    public ProductOptionDto getProductOption() { return productOption; }
    public void setProductOption(ProductOptionDto productOption) { this.productOption = productOption; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getImgUrl() { return imgUrl; }
    public void setImgUrl(String imgUrl) { this.imgUrl = imgUrl; }

    public Boolean getEnable() { return enable; }
    public void setEnable(Boolean enable) { this.enable = enable; }
}
