package ptithcm.graduation.apigateway.services.baseService.product.business;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import ptithcm.graduation.apigateway.services.baseService.product.interfaces.IProductHeartService;
import ptithcm.graduation.apigateway.services.baseService.product.mapper.ProductHeartMapper;
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
public class ProductHeartBusinessService implements IProductHeartService {

    @GrpcClient("product-heart-service")
    private ProductHeartServiceGrpc.ProductHeartServiceFutureStub productHeartServiceFutureStub;
    
    @GrpcClient("product-heart-service")
    private ProductHeartServiceGrpc.ProductHeartServiceBlockingStub productHeartServiceBlockingStub;

    @Autowired
    private ProductHeartMapper productHeartMapper;

    @Override
    public CompletableFuture<List<ProductHeartDto>> getProductHeartListByProductId(String productId) {
        UUIDValue request = UUIDValue.newBuilder().setId(productId).build();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Iterator<ProductHeart> responseIterator = productHeartServiceBlockingStub.getProductHeartListByProductId(request);
                List<ProductHeartDto> hearts = new ArrayList<>();
                while (responseIterator.hasNext()) {
                    hearts.add(productHeartMapper.toDto(responseIterator.next()));
                }
                return hearts;
            } catch (Exception e) {
                throw new RuntimeException("Error getting product hearts by product id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<ProductHeartDto> createProductHeart(String productId, String customerId) {
        ProductHeartDto dto = new ProductHeartDto();
        dto.setProductId(productId);
        dto.setCustomerId(customerId);
        
        CreateProductHeartRequest grpcRequest = productHeartMapper.toCreateProto(dto);
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProductHeart response = productHeartServiceFutureStub.createProductHeart(grpcRequest).get();
                return productHeartMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error creating product heart: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProductHeartById(Long id) {
        ProductHeartId request = productHeartMapper.toIdProto(id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                productHeartServiceFutureStub.deleteProductHeartById(request).get();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Error deleting product heart by id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProductHeartList(List<ProductHeartDto> hearts) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Note: This would require streaming ProductHeart objects
                // Implementation would depend on the specific gRPC streaming setup
                throw new UnsupportedOperationException("Streaming delete not implemented yet");
            } catch (Exception e) {
                throw new RuntimeException("Error deleting product heart list: " + e.getMessage(), e);
            }
        });
    }
}
