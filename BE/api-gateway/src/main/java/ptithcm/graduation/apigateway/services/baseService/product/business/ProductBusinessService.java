package ptithcm.graduation.apigateway.services.baseService.product.business;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import ptithcm.graduation.apigateway.services.baseService.product.interfaces.IProductService;
import ptithcm.graduation.apigateway.services.baseService.product.mapper.ProductMapper;
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
public class ProductBusinessService implements IProductService {

    @GrpcClient("product-service")
    private ProductServiceGrpc.ProductServiceFutureStub productServiceFutureStub;
    
    @GrpcClient("product-service")
    private ProductServiceGrpc.ProductServiceBlockingStub productServiceBlockingStub;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public CompletableFuture<ProductDto> getProductById(String id) {
        UUIDValue request = productMapper.toUUIDValueProto(id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                Product response = productServiceFutureStub.getProductById(request).get();
                return productMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error getting product by id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<List<ProductDto>> getProductListByStaffId(String staffId) {
        UUIDValue request = productMapper.toUUIDValueProto(staffId);
        return CompletableFuture.supplyAsync(() -> {
            try {
                Iterator<Product> responseIterator = productServiceBlockingStub.getProductListByStaffId(request);
                List<ProductDto> products = new ArrayList<>();
                while (responseIterator.hasNext()) {
                    products.add(productMapper.toDto(responseIterator.next()));
                }
                return products;
            } catch (Exception e) {
                throw new RuntimeException("Error getting products by staff id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<List<ProductDto>> getProductListByStoreId(String storeId) {
        UUIDValue request = productMapper.toUUIDValueProto(storeId);
        return CompletableFuture.supplyAsync(() -> {
            try {
                Iterator<Product> responseIterator = productServiceBlockingStub.getProductListByStoreId(request);
                List<ProductDto> products = new ArrayList<>();
                while (responseIterator.hasNext()) {
                    products.add(productMapper.toDto(responseIterator.next()));
                }
                return products;
            } catch (Exception e) {
                throw new RuntimeException("Error getting products by store id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<ProductDto> createProduct(CreateProductRequestDto request) {
        CreateProductRequest grpcRequest = productMapper.toProto(request);
        return CompletableFuture.supplyAsync(() -> {
            try {
                Product response = productServiceFutureStub.createProduct(grpcRequest).get();
                return productMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error creating product: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<ProductDto> updateProduct(UpdateProductRequestDto request) {
        UpdateProductRequest grpcRequest = productMapper.toProto(request);
        return CompletableFuture.supplyAsync(() -> {
            try {
                Product response = productServiceFutureStub.updateProduct(grpcRequest).get();
                return productMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error updating product: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProductById(String id) {
        UUIDValue request = productMapper.toUUIDValueProto(id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                productServiceFutureStub.deleteProductById(request).get();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Error deleting product by id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProductListByStoreId(String storeId) {
        UUIDValue request = productMapper.toUUIDValueProto(storeId);
        return CompletableFuture.supplyAsync(() -> {
            try {
                productServiceFutureStub.deleteProductListByStoreId(request).get();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Error deleting products by store id: " + e.getMessage(), e);
            }
        });
    }
}
