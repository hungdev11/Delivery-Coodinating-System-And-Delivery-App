package ptithcm.graduation.apigateway.services.baseService.product.dto;

import java.time.Instant;

public class ProductHeartDto {
    private Long id;
    private String productId;
    private String customerId;
    private Instant createdAt;

    // Default constructor
    public ProductHeartDto() {}

    // All args constructor
    public ProductHeartDto(Long id, String productId, String customerId, Instant createdAt) {
        this.id = id;
        this.productId = productId;
        this.customerId = customerId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
