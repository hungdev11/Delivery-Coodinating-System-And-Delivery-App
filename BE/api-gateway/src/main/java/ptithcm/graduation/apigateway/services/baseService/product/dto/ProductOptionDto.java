package ptithcm.graduation.apigateway.services.baseService.product.dto;

public class ProductOptionDto {
    private Long id;
    private String productId;
    private ProductDto product;
    private String sku;
    private String name;
    private Double priceRetail;
    private Double priceSale;
    private Integer totalWaitingShip;
    private Boolean enable;

    // Default constructor
    public ProductOptionDto() {}

    // All args constructor
    public ProductOptionDto(Long id, String productId, ProductDto product, String sku, 
                           String name, Double priceRetail, Double priceSale, 
                           Integer totalWaitingShip, Boolean enable) {
        this.id = id;
        this.productId = productId;
        this.product = product;
        this.sku = sku;
        this.name = name;
        this.priceRetail = priceRetail;
        this.priceSale = priceSale;
        this.totalWaitingShip = totalWaitingShip;
        this.enable = enable;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public ProductDto getProduct() { return product; }
    public void setProduct(ProductDto product) { this.product = product; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPriceRetail() { return priceRetail; }
    public void setPriceRetail(Double priceRetail) { this.priceRetail = priceRetail; }

    public Double getPriceSale() { return priceSale; }
    public void setPriceSale(Double priceSale) { this.priceSale = priceSale; }

    public Integer getTotalWaitingShip() { return totalWaitingShip; }
    public void setTotalWaitingShip(Integer totalWaitingShip) { this.totalWaitingShip = totalWaitingShip; }

    public Boolean getEnable() { return enable; }
    public void setEnable(Boolean enable) { this.enable = enable; }
}
