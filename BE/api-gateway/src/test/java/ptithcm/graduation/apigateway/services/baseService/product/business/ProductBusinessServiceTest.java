package ptithcm.graduation.apigateway.services.baseService.product.business;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import ptithcm.graduation.apigateway.services.baseService.product.mapper.ProductMapper;
import products.*;
import commons.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.Iterator;
import java.util.UUID;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Futures;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductBusinessServiceTest {

    @Mock
    private ProductServiceGrpc.ProductServiceFutureStub productServiceFutureStub;

    @Mock
    private ProductServiceGrpc.ProductServiceBlockingStub productServiceBlockingStub;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductBusinessService productBusinessService;

    private Product mockProduct;
    private ProductDto mockProductDto;
    private UUIDValue mockUuidValue;
    private CreateProductRequestDto mockCreateRequest;
    private UpdateProductRequestDto mockUpdateRequest;

    @BeforeEach
    void setUp() {
        // Setup mocks
        ReflectionTestUtils.setField(productBusinessService, "productServiceFutureStub", productServiceFutureStub);
        ReflectionTestUtils.setField(productBusinessService, "productServiceBlockingStub", productServiceBlockingStub);
        
        // Mock data
        String productId = UUID.randomUUID().toString();
        mockProduct = Product.newBuilder()
                .setId(productId)
                .setTitle("Test Product")
                .setDescription("Test Description")
                .setStoreId("store-123")
                .setStaffId("staff-123")
                .setImgLink("test-image.jpg")
                .setEnable(true)
                .build();

        mockProductDto = new ProductDto();
        mockProductDto.setId(productId);
        mockProductDto.setTitle("Test Product");
        mockProductDto.setDescription("Test Description");
        mockProductDto.setStoreId("store-123");
        mockProductDto.setStaffId("staff-123");
        mockProductDto.setImgLink("test-image.jpg");
        mockProductDto.setEnable(true);

        mockUuidValue = UUIDValue.newBuilder()
                .setId(productId)
                .build();

        mockCreateRequest = new CreateProductRequestDto();
        mockCreateRequest.setName("New Product");
        mockCreateRequest.setStoreId("store-123");
        mockCreateRequest.setStaffId("staff-123");
        mockCreateRequest.setImgLink("new-image.jpg");

        mockUpdateRequest = new UpdateProductRequestDto();
        mockUpdateRequest.setId(productId);
        mockUpdateRequest.setTitle("Updated Product");
        mockUpdateRequest.setDescription("Updated Description");
        mockUpdateRequest.setStoreId("store-123");
        mockUpdateRequest.setStaffId("staff-123");
        mockUpdateRequest.setImgLink("updated-image.jpg");
        mockUpdateRequest.setEnable(true);
    }

    @Test
    void getProductById_Success() throws Exception {
        // Arrange
        String productId = UUID.randomUUID().toString();
        when(productMapper.toUUIDValueProto(productId)).thenReturn(mockUuidValue);
        when(productServiceFutureStub.getProductById(any(UUIDValue.class)))
                .thenReturn(Futures.immediateFuture(mockProduct));
        when(productMapper.toDto(mockProduct)).thenReturn(mockProductDto);

        // Act
        CompletableFuture<ProductDto> result = productBusinessService.getProductById(productId);

        // Assert
        assertNotNull(result);
        ProductDto actualResult = result.get();
        assertEquals(mockProductDto.getId(), actualResult.getId());
        assertEquals(mockProductDto.getTitle(), actualResult.getTitle());
        assertEquals(mockProductDto.getDescription(), actualResult.getDescription());
        
        verify(productMapper).toUUIDValueProto(productId);
        verify(productServiceFutureStub).getProductById(mockUuidValue);
        verify(productMapper).toDto(mockProduct);
    }

    @Test
    void getProductById_ThrowsException() throws Exception {
        // Arrange
        String productId = UUID.randomUUID().toString();
        when(productMapper.toUUIDValueProto(productId)).thenReturn(mockUuidValue);
        when(productServiceFutureStub.getProductById(any(UUIDValue.class)))
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("gRPC Error")));

        // Act & Assert
        CompletableFuture<ProductDto> result = productBusinessService.getProductById(productId);
        
        ExecutionException executionException = assertThrows(ExecutionException.class, () -> result.get());
        assertTrue(executionException.getCause() instanceof RuntimeException);
        assertEquals("Error getting product by id: java.lang.RuntimeException: gRPC Error", executionException.getCause().getMessage());
        
        verify(productMapper).toUUIDValueProto(productId);
        verify(productServiceFutureStub).getProductById(mockUuidValue);
    }

    @Test
    void getProductListByStaffId_Success() throws Exception {
        // Arrange
        String staffId = UUID.randomUUID().toString();
        List<Product> mockProducts = List.of(mockProduct);
        Iterator<Product> mockIterator = mockProducts.iterator();
        
        when(productMapper.toUUIDValueProto(staffId)).thenReturn(mockUuidValue);
        when(productServiceBlockingStub.getProductListByStaffId(any(UUIDValue.class)))
                .thenReturn(mockIterator);
        when(productMapper.toDto(mockProduct)).thenReturn(mockProductDto);

        // Act
        CompletableFuture<List<ProductDto>> result = productBusinessService.getProductListByStaffId(staffId);

        // Assert
        assertNotNull(result);
        List<ProductDto> actualResult = result.get();
        assertEquals(1, actualResult.size());
        assertEquals(mockProductDto.getId(), actualResult.get(0).getId());
        
        verify(productMapper).toUUIDValueProto(staffId);
        verify(productServiceBlockingStub).getProductListByStaffId(mockUuidValue);
        verify(productMapper).toDto(mockProduct);
    }

    @Test
    void getProductListByStoreId_Success() throws Exception {
        // Arrange
        String storeId = UUID.randomUUID().toString();
        List<Product> mockProducts = List.of(mockProduct);
        Iterator<Product> mockIterator = mockProducts.iterator();
        
        when(productMapper.toUUIDValueProto(storeId)).thenReturn(mockUuidValue);
        when(productServiceBlockingStub.getProductListByStoreId(any(UUIDValue.class)))
                .thenReturn(mockIterator);
        when(productMapper.toDto(mockProduct)).thenReturn(mockProductDto);

        // Act
        CompletableFuture<List<ProductDto>> result = productBusinessService.getProductListByStoreId(storeId);

        // Assert
        assertNotNull(result);
        List<ProductDto> actualResult = result.get();
        assertEquals(1, actualResult.size());
        assertEquals(mockProductDto.getId(), actualResult.get(0).getId());
        
        verify(productMapper).toUUIDValueProto(storeId);
        verify(productServiceBlockingStub).getProductListByStoreId(mockUuidValue);
        verify(productMapper).toDto(mockProduct);
    }

    @Test
    void createProduct_Success() throws Exception {
        // Arrange
        CreateProductRequest mockGrpcRequest = CreateProductRequest.newBuilder()
                .setName("New Product")
                .setStoreId("store-123")
                .setStaffId("staff-123")
                .setImgLink("new-image.jpg")
                .build();
        
        when(productMapper.toProto(mockCreateRequest)).thenReturn(mockGrpcRequest);
        when(productServiceFutureStub.createProduct(any(CreateProductRequest.class)))
                .thenReturn(Futures.immediateFuture(mockProduct));
        when(productMapper.toDto(mockProduct)).thenReturn(mockProductDto);

        // Act
        CompletableFuture<ProductDto> result = productBusinessService.createProduct(mockCreateRequest);

        // Assert
        assertNotNull(result);
        ProductDto actualResult = result.get();
        assertEquals(mockProductDto.getId(), actualResult.getId());
        
        verify(productMapper).toProto(mockCreateRequest);
        verify(productServiceFutureStub).createProduct(mockGrpcRequest);
        verify(productMapper).toDto(mockProduct);
    }

    @Test
    void updateProduct_Success() throws Exception {
        // Arrange
        UpdateProductRequest mockGrpcRequest = UpdateProductRequest.newBuilder()
                .setId(mockUpdateRequest.getId())
                .setTitle("Updated Product")
                .setDescription("Updated Description")
                .setStoreId("store-123")
                .setStaffId("staff-123")
                .setImgLink("updated-image.jpg")
                .setEnable(true)
                .build();
        
        when(productMapper.toProto(mockUpdateRequest)).thenReturn(mockGrpcRequest);
        when(productServiceFutureStub.updateProduct(any(UpdateProductRequest.class)))
                .thenReturn(Futures.immediateFuture(mockProduct));
        when(productMapper.toDto(mockProduct)).thenReturn(mockProductDto);

        // Act
        CompletableFuture<ProductDto> result = productBusinessService.updateProduct(mockUpdateRequest);

        // Assert
        assertNotNull(result);
        ProductDto actualResult = result.get();
        assertEquals(mockProductDto.getId(), actualResult.getId());
        
        verify(productMapper).toProto(mockUpdateRequest);
        verify(productServiceFutureStub).updateProduct(mockGrpcRequest);
        verify(productMapper).toDto(mockProduct);
    }

    @Test
    void createProduct_ThrowsException() throws Exception {
        // Arrange
        CreateProductRequest mockGrpcRequest = CreateProductRequest.newBuilder()
                .setName("New Product")
                .setStoreId("store-123")
                .setStaffId("staff-123")
                .setImgLink("new-image.jpg")
                .build();
        
        when(productMapper.toProto(mockCreateRequest)).thenReturn(mockGrpcRequest);
        when(productServiceFutureStub.createProduct(any(CreateProductRequest.class)))
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("gRPC Error")));

        // Act & Assert
        CompletableFuture<ProductDto> result = productBusinessService.createProduct(mockCreateRequest);
        
        ExecutionException executionException = assertThrows(ExecutionException.class, () -> result.get());
        assertTrue(executionException.getCause() instanceof RuntimeException);
        assertEquals("Error creating product: java.lang.RuntimeException: gRPC Error", executionException.getCause().getMessage());
        
        verify(productMapper).toProto(mockCreateRequest);
        verify(productServiceFutureStub).createProduct(mockGrpcRequest);
    }
}
