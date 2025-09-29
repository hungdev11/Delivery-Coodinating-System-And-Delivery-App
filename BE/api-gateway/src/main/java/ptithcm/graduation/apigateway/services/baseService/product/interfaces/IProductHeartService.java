package ptithcm.graduation.apigateway.services.baseService.product.interfaces;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IProductHeartService {
    
    // ProductHeart operations
    CompletableFuture<List<ProductHeartDto>> getProductHeartListByProductId(String productId);
    CompletableFuture<ProductHeartDto> createProductHeart(String productId, String customerId);
    CompletableFuture<Void> deleteProductHeartById(Long id);
    CompletableFuture<Void> deleteProductHeartList(List<ProductHeartDto> hearts);
}
