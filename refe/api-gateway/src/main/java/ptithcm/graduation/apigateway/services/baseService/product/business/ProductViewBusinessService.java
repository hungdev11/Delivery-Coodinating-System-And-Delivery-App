package ptithcm.graduation.apigateway.services.baseService.product.business;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import ptithcm.graduation.apigateway.services.baseService.product.interfaces.IProductViewService;
import ptithcm.graduation.apigateway.services.baseService.product.mapper.ProductViewMapper;
import products.*;
import commons.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.devh.boot.grpc.client.inject.GrpcClient;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.Iterator;

@Service
public class ProductViewBusinessService implements IProductViewService {

    @GrpcClient("product-view-service")
    private ProductViewServiceGrpc.ProductViewServiceFutureStub productViewServiceFutureStub;
    
    @GrpcClient("product-view-service")
    private ProductViewServiceGrpc.ProductViewServiceBlockingStub productViewServiceBlockingStub;

    @Autowired
    private ProductViewMapper productViewMapper;

    @Override
    public CompletableFuture<List<ProductViewDto>> getProductViewListByProductId(String productId) {
        UUIDValue request = UUIDValue.newBuilder().setId(productId).build();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Iterator<ProductView> responseIterator = productViewServiceBlockingStub.getProductViewListByProductId(request);
                List<ProductViewDto> views = new ArrayList<>();
                while (responseIterator.hasNext()) {
                    views.add(productViewMapper.toDto(responseIterator.next()));
                }
                return views;
            } catch (Exception e) {
                throw new RuntimeException("Error getting product views by product id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<ProductViewDto> createProductView(String productId, String customerId) {
        ProductViewDto dto = new ProductViewDto();
        dto.setProductId(productId);
        dto.setCustomerId(customerId);
        
        CreateProductViewRequest grpcRequest = productViewMapper.toCreateProto(dto);
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProductView response = productViewServiceFutureStub.createProductView(grpcRequest).get();
                return productViewMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error creating product view: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProductViewList(List<Long> ids) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Note: This would require streaming ProductViewId objects
                // Implementation would depend on the specific gRPC streaming setup
                throw new UnsupportedOperationException("Streaming delete not implemented yet");
            } catch (Exception e) {
                throw new RuntimeException("Error deleting product view list: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProductViewById(Long id) {
        ProductViewId request = productViewMapper.toIdProto(id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                productViewServiceFutureStub.deleteProductViewById(request).get();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Error deleting product view by id: " + e.getMessage(), e);
            }
        });
    }
}
