package ptithcm.graduation.apigateway.services.baseService.product.mapper;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import products.*;
import org.springframework.stereotype.Component;
import com.google.protobuf.Timestamp;
import java.time.Instant;

@Component
public class ProductHeartMapper {

    // Proto to DTO mappings
    public ProductHeartDto toDto(ProductHeart proto) {
        if (proto == null) return null;
        
        return new ProductHeartDto(
            proto.getId(),
            proto.getProductId(),
            proto.getCustomerId(),
            toInstant(proto.getCreatedAt())
        );
    }

    // DTO to Proto mappings
    public CreateProductHeartRequest toCreateProto(ProductHeartDto dto) {
        if (dto == null) return null;
        
        return CreateProductHeartRequest.newBuilder()
            .setProductId(dto.getProductId() != null ? dto.getProductId() : "")
            .setCustomerId(dto.getCustomerId() != null ? dto.getCustomerId() : "")
            .build();
    }

    public ProductHeartId toIdProto(Long id) {
        return ProductHeartId.newBuilder()
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
