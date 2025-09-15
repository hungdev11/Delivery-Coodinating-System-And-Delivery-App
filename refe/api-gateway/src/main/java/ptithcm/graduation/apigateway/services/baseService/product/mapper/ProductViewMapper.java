package ptithcm.graduation.apigateway.services.baseService.product.mapper;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import products.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.protobuf.Timestamp;
import java.time.Instant;

@Component
public class ProductViewMapper {

    @Autowired
    private ProductMapper productMapper;

    // Proto to DTO mappings
    public ProductViewDto toDto(ProductView proto) {
        if (proto == null) return null;
        
        ProductViewDto dto = new ProductViewDto();
        dto.setId(proto.getId());
        dto.setCustomerId(proto.getCustomerId());
        dto.setCreatedAt(toInstant(proto.getCreatedAt()));
        
        // Handle oneof productRef
        switch (proto.getProductRefCase()) {
            case PRODUCT:
                dto.setProduct(productMapper.toDto(proto.getProduct()));
                break;
            case PRODUCTID:
                dto.setProductId(proto.getProductId());
                break;
            default:
                break;
        }
        
        return dto;
    }

    // DTO to Proto mappings
    public CreateProductViewRequest toCreateProto(ProductViewDto dto) {
        if (dto == null) return null;
        
        CreateProductViewRequest.Builder builder = CreateProductViewRequest.newBuilder()
            .setCustomerId(dto.getCustomerId() != null ? dto.getCustomerId() : "");
        
        if (dto.getProductId() != null) {
            builder.setProductId(dto.getProductId());
        } else if (dto.getProduct() != null) {
            // Note: Would need to convert ProductDto back to Product proto
        }
        
        return builder.build();
    }

    public ProductViewId toIdProto(Long id) {
        return ProductViewId.newBuilder()
            .setId(id != null ? id : 0L)
            .build();
    }

    // Helper methods
    private Instant toInstant(Timestamp timestamp) {
        if (timestamp == null) return null;
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    private Timestamp toTimestamp(Instant instant) {
        if (instant == null) return null;
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }
}
