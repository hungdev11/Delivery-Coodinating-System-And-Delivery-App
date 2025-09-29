package ptithcm.graduation.apigateway.controllers.v1.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ptithcm.graduation.apigateway.annotations.AuthRequired;
import ptithcm.graduation.apigateway.annotations.PublicRoute;
import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import ptithcm.graduation.apigateway.services.baseService.product.interfaces.IProductService;
import ptithcm.graduation.apigateway.services.baseService.product.interfaces.IProductOptionService;
import ptithcm.graduation.apigateway.services.baseService.product.interfaces.IProductLangService;
import ptithcm.graduation.apigateway.services.baseService.product.interfaces.IProductHeartService;
import ptithcm.graduation.apigateway.services.baseService.product.interfaces.IProductViewService;
import ptithcm.graduation.apigateway.services.baseService.product.interfaces.IAttributeService;
import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
// Mặc định: yêu cầu phải đăng nhập
@AuthRequired
public class ProductController {

    private final IProductService productService;
    private final IProductOptionService productOptionService;
    private final IProductLangService productLangService;
    private final IProductHeartService productHeartService;
    private final IProductViewService productViewService;
    private final IAttributeService attributeService;

    /**
     * Get product by ID
     */
    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<ProductDto>> getProductById(@PathVariable String id) {
        log.info("Get product request received for ID: {}", id);
        return productService.getProductById(id)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to get product with ID: {}", id, throwable);
                    return ResponseEntity.badRequest().build();
                });
    }

    /**
     * Get product list by staff ID
     */
    @GetMapping("/staff/{staffId}")
    public CompletableFuture<ResponseEntity<List<ProductDto>>> getProductListByStaffId(@PathVariable String staffId) {
        log.info("Get product list by staff ID request received: {}", staffId);
        return productService.getProductListByStaffId(staffId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to get product list by staff ID: {}", staffId, throwable);
                    return ResponseEntity.badRequest().build();
                });
    }

    /**
     * Get product list by store ID
     */
    @GetMapping("/store/{storeId}")
    public CompletableFuture<ResponseEntity<List<ProductDto>>> getProductListByStoreId(@PathVariable String storeId) {
        log.info("Get product list by store ID request received: {}", storeId);
        return productService.getProductListByStoreId(storeId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to get product list by store ID: {}", storeId, throwable);
                    return ResponseEntity.badRequest().build();
                });
    }

    /**
     * Create new product
     */
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping
    public CompletableFuture<ResponseEntity<ProductDto>> createProduct(@Valid @RequestBody CreateProductRequestDto request) {
        log.info("Create product request received for name: {}", request.getName());
        return productService.createProduct(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to create product: {}", request.getName(), throwable);
                    return ResponseEntity.badRequest().build();
                });
    }

    /**
     * Update product
     */
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<ProductDto>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody UpdateProductRequestDto request) {
        log.info("Update product request received for ID: {}", id);
        
        // Set the ID from path variable
        request.setId(id);
        
        return productService.updateProduct(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to update product with ID: {}", id, throwable);
                    return ResponseEntity.badRequest().build();
                });
    }

    /**
     * Delete product
     */
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public CompletableFuture<ResponseEntity<Void>> deleteProduct(@PathVariable String id) {
        log.info("Delete product request received for ID: {}", id);
        return productService.deleteProductById(id)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to delete product with ID: {}", id, throwable);
                    return ResponseEntity.badRequest().build();
                });
    }

    /**
     * Get product options by product ID
     */
    @GetMapping("/{productId}/options")
    public CompletableFuture<ResponseEntity<List<ProductOptionDto>>> getProductOptions(@PathVariable String productId) {
        log.info("Get product options request received for product ID: {}", productId);
        return productOptionService.getProductOptionListByProductId(productId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to get product options for product ID: {}", productId, throwable);
                    return ResponseEntity.badRequest().build();
                });
    }

    /**
     * Get product languages by product ID
     */
    @GetMapping("/{productId}/languages")
    public CompletableFuture<ResponseEntity<List<ProductLangDto>>> getProductLanguages(@PathVariable String productId) {
        log.info("Get product languages request received for product ID: {}", productId);
        return productLangService.getProductLangListByProductId(productId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to get product languages for product ID: {}", productId, throwable);
                    return ResponseEntity.badRequest().build();
                });
    }

    /**
     * Get product hearts by product ID
     */
    @GetMapping("/{productId}/hearts")
    public CompletableFuture<ResponseEntity<List<ProductHeartDto>>> getProductHearts(@PathVariable String productId) {
        log.info("Get product hearts request received for product ID: {}", productId);
        return productHeartService.getProductHeartListByProductId(productId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to get product hearts for product ID: {}", productId, throwable);
                    return ResponseEntity.badRequest().build();
                });
    }

    /**
     * Get product views by product ID
     */
    @GetMapping("/{productId}/views")
    public CompletableFuture<ResponseEntity<List<ProductViewDto>>> getProductViews(@PathVariable String productId) {
        log.info("Get product views request received for product ID: {}", productId);
        return productViewService.getProductViewListByProductId(productId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to get product views for product ID: {}", productId, throwable);
                    return ResponseEntity.badRequest().build();
                });
    }

    /**
     * Get attributes by product option ID
     */
    @GetMapping("/options/{productOptionId}/attributes")
    public CompletableFuture<ResponseEntity<List<AttributeDto>>> getProductOptionAttributes(@PathVariable String productOptionId) {
        log.info("Get product option attributes request received for product option ID: {}", productOptionId);
        return attributeService.getAttributeListByProductOptionId(productOptionId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to get product option attributes for product option ID: {}", productOptionId, throwable);
                    return ResponseEntity.badRequest().build();
                });
    }

    /**
     * Health check endpoint
     */
    @PublicRoute
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Product service is running");
    }
}
