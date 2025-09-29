package ptithcm.graduation.apigateway.services.baseService.product.dto;

import java.time.Instant;

public class ProductViewDto {
    private Long id;
    private ProductDto product;
    private String productId;
    private String customerId;
    private Instant createdAt;

    // Default constructor
    public ProductViewDto() {}

    // All args constructor
    public ProductViewDto(Long id, ProductDto product, String productId, String customerId, Instant createdAt) {
        this.id = id;
        this.product = product;
        this.productId = productId;
        this.customerId = customerId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProductDto getProduct() { return product; }
    public void setProduct(ProductDto product) { this.product = product; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
