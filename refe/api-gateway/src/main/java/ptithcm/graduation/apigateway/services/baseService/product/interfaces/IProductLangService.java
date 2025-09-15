package ptithcm.graduation.apigateway.services.baseService.product.interfaces;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IProductLangService {
    
    // ProductLang operations
    CompletableFuture<List<ProductLangDto>> getProductLangListByProductId(String productId);
    CompletableFuture<ProductLangDto> getProductLangById(Long id);
    CompletableFuture<ProductLangDto> createProductLang(String productId, String title, 
                                                        String content, String description, String langCode);
    CompletableFuture<Void> deleteProductLangList(List<ProductLangDto> langs);
    CompletableFuture<Void> deleteProductLangListByProductId(String productId);
}
