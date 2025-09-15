package ptithcm.graduation.apigateway.services.baseService.product.business;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import ptithcm.graduation.apigateway.services.baseService.product.interfaces.IProductOptionService;
import ptithcm.graduation.apigateway.services.baseService.product.mapper.ProductOptionMapper;
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
public class ProductOptionBusinessService implements IProductOptionService {

    @GrpcClient("product-option-service")
    private ProductOptionServiceGrpc.ProductOptionServiceFutureStub productOptionServiceFutureStub;
    
    @GrpcClient("product-option-service")
    private ProductOptionServiceGrpc.ProductOptionServiceBlockingStub productOptionServiceBlockingStub;

    @Autowired
    private ProductOptionMapper productOptionMapper;

    @Override
    public CompletableFuture<List<ProductOptionDto>> getProductOptionListByProductId(String productId) {
        UUIDValue request = UUIDValue.newBuilder().setId(productId).build();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Iterator<ProductOption> responseIterator = productOptionServiceBlockingStub.getProductOptionListByProductId(request);
                List<ProductOptionDto> options = new ArrayList<>();
                while (responseIterator.hasNext()) {
                    options.add(productOptionMapper.toDto(responseIterator.next()));
                }
                return options;
            } catch (Exception e) {
                throw new RuntimeException("Error getting product options by product id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<ProductOptionDto> getProductOptionById(Long id) {
        ProductOptionId request = productOptionMapper.toIdProto(id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProductOption response = productOptionServiceFutureStub.getProductOptionById(request).get();
                return productOptionMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error getting product option by id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<ProductOptionDto> createProductOption(ProductOptionDto request) {
        CreateProductOptionRequest grpcRequest = productOptionMapper.toCreateProto(request);
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProductOption response = productOptionServiceFutureStub.createProductOption(grpcRequest).get();
                return productOptionMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error creating product option: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<ProductOptionDto> updateProductOption(ProductOptionDto request) {
        UpdateProductOptionRequest grpcRequest = productOptionMapper.toUpdateProto(request);
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProductOption response = productOptionServiceFutureStub.updateProductOption(grpcRequest).get();
                return productOptionMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error updating product option: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProductOptionList(List<Long> ids) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Note: This would require streaming the ProductOptionId objects
                // Implementation would depend on the specific gRPC streaming setup
                throw new UnsupportedOperationException("Streaming delete not implemented yet");
            } catch (Exception e) {
                throw new RuntimeException("Error deleting product option list: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProductOptionById(Long id) {
        ProductOptionId request = productOptionMapper.toIdProto(id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                productOptionServiceFutureStub.deleteProductOptionById(request).get();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Error deleting product option by id: " + e.getMessage(), e);
            }
        });
    }
}
