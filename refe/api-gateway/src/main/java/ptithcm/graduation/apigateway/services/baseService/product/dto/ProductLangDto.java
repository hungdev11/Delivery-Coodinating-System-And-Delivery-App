package ptithcm.graduation.apigateway.services.baseService.product.dto;

public class ProductLangDto {
    private Long id;
    private String productId;
    private String title;
    private String content;
    private String description;

    // Default constructor
    public ProductLangDto() {}

    // All args constructor
    public ProductLangDto(Long id, String productId, String title, String content, String description) {
        this.id = id;
        this.productId = productId;
        this.title = title;
        this.content = content;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
