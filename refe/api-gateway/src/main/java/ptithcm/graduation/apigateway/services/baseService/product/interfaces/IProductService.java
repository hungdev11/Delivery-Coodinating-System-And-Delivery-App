package ptithcm.graduation.apigateway.services.baseService.product.interfaces;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IProductService {
    
    // Product CRUD operations
    CompletableFuture<ProductDto> getProductById(String id);
    CompletableFuture<List<ProductDto>> getProductListByStaffId(String staffId);
    CompletableFuture<List<ProductDto>> getProductListByStoreId(String storeId);
    CompletableFuture<ProductDto> createProduct(CreateProductRequestDto request);
    CompletableFuture<ProductDto> updateProduct(UpdateProductRequestDto request);
    CompletableFuture<Void> deleteProductById(String id);
    CompletableFuture<Void> deleteProductListByStoreId(String storeId);
}
