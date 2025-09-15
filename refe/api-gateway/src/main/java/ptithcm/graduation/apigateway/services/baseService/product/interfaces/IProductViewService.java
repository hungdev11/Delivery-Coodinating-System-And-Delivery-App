package ptithcm.graduation.apigateway.services.baseService.product.interfaces;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IProductViewService {
    
    // ProductView operations
    CompletableFuture<List<ProductViewDto>> getProductViewListByProductId(String productId);
    CompletableFuture<ProductViewDto> createProductView(String productId, String customerId);
    CompletableFuture<Void> deleteProductViewList(List<Long> ids);
    CompletableFuture<Void> deleteProductViewById(Long id);
}
