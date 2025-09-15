package ptithcm.graduation.apigateway.services.baseService.product.interfaces;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IProductOptionService {
    
    // ProductOption CRUD operations
    CompletableFuture<List<ProductOptionDto>> getProductOptionListByProductId(String productId);
    CompletableFuture<ProductOptionDto> getProductOptionById(Long id);
    CompletableFuture<ProductOptionDto> createProductOption(ProductOptionDto request);
    CompletableFuture<ProductOptionDto> updateProductOption(ProductOptionDto request);
    CompletableFuture<Void> deleteProductOptionList(List<Long> ids);
    CompletableFuture<Void> deleteProductOptionById(Long id);
}
