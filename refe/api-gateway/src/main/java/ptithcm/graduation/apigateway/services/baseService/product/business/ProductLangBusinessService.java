package ptithcm.graduation.apigateway.services.baseService.product.business;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import ptithcm.graduation.apigateway.services.baseService.product.interfaces.IProductLangService;
import products.*;
import commons.*;
import org.springframework.stereotype.Service;
import net.devh.boot.grpc.client.inject.GrpcClient;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.Iterator;

@Service
public class ProductLangBusinessService implements IProductLangService {

    @GrpcClient("product-lang-service")
    private ProductLangServiceGrpc.ProductLangServiceFutureStub productLangServiceFutureStub;
    
    @GrpcClient("product-lang-service")
    private ProductLangServiceGrpc.ProductLangServiceBlockingStub productLangServiceBlockingStub;

    @Override
    public CompletableFuture<List<ProductLangDto>> getProductLangListByProductId(String productId) {
        UUIDValue request = UUIDValue.newBuilder().setId(productId).build();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Iterator<ProductLang> responseIterator = productLangServiceBlockingStub.getProductLangListByProductId(request);
                List<ProductLangDto> langs = new ArrayList<>();
                while (responseIterator.hasNext()) {
                    ProductLang lang = responseIterator.next();
                    ProductLangDto dto = new ProductLangDto(
                        lang.getId(),
                        lang.getProductId(),
                        lang.getTitle(),
                        lang.getContent(),
                        lang.getDescription()
                    );
                    langs.add(dto);
                }
                return langs;
            } catch (Exception e) {
                throw new RuntimeException("Error getting product langs by product id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<ProductLangDto> getProductLangById(Long id) {
        ProductLangId request = ProductLangId.newBuilder().setId(id).build();
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProductLang response = productLangServiceFutureStub.getProductLangById(request).get();
                return new ProductLangDto(
                    response.getId(),
                    response.getProductId(),
                    response.getTitle(),
                    response.getContent(),
                    response.getDescription()
                );
            } catch (Exception e) {
                throw new RuntimeException("Error getting product lang by id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<ProductLangDto> createProductLang(String productId, String title, 
                                                              String content, String description, String langCode) {
        CreateProductLangRequest request = CreateProductLangRequest.newBuilder()
            .setProductId(productId != null ? productId : "")
            .setTitle(title != null ? title : "")
            .setContent(content != null ? content : "")
            .setDescription(description != null ? description : "")
            .setLangCode(langCode != null ? langCode : "")
            .build();
            
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProductLang response = productLangServiceFutureStub.createProductLang(request).get();
                return new ProductLangDto(
                    response.getId(),
                    response.getProductId(),
                    response.getTitle(),
                    response.getContent(),
                    response.getDescription()
                );
            } catch (Exception e) {
                throw new RuntimeException("Error creating product lang: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProductLangList(List<ProductLangDto> langs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Note: This would require streaming ProductLang objects
                // Implementation would depend on the specific gRPC streaming setup
                throw new UnsupportedOperationException("Streaming delete not implemented yet");
            } catch (Exception e) {
                throw new RuntimeException("Error deleting product lang list: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProductLangListByProductId(String productId) {
        UUIDValue request = UUIDValue.newBuilder().setId(productId).build();
        return CompletableFuture.supplyAsync(() -> {
            try {
                productLangServiceFutureStub.deleteProductLangListByProductId(request).get();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Error deleting product lang list by product id: " + e.getMessage(), e);
            }
        });
    }
}
