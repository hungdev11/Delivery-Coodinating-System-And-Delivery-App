package ptithcm.graduation.apigateway.services.baseService.product.mapper;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import products.*;
import commons.*;
import org.springframework.stereotype.Component;
import com.google.protobuf.Timestamp;
import java.time.Instant;

@Component
public class ProductMapper {

    // Proto to DTO mappings
    public ProductDto toDto(Product proto) {
        if (proto == null) return null;
        
        return new ProductDto(
            proto.getId(),
            proto.getStoreId(),
            proto.getStaffId(),
            proto.getTitle(),
            proto.getContent(),
            proto.getDescription(),
            proto.getImgLink(),
            proto.getTotalWaitingShip(),
            proto.getTotalHearts(),
            proto.getTotalViews(),
            proto.getLangCode(),
            proto.getEnable(),
            proto.hasCreatedAt() ? toInstant(proto.getCreatedAt()) : null,
            proto.hasUpdatedAt() ? toInstant(proto.getUpdatedAt()) : null
        );
    }

    public CommonDto.PageRequestDto toDto(commons.PageRequest proto) {
        if (proto == null) return null;
        
        return new CommonDto.PageRequestDto(
            proto.getPage(),
            proto.getSize()
        );
    }

    public CommonDto.PageInfoDto toDto(commons.PageInfo proto) {
        if (proto == null) return null;
        
        return new CommonDto.PageInfoDto(
            proto.getPage(),
            proto.getSize(),
            proto.getTotalPages(),
            proto.getTotalElements()
        );
    }

    public CommonDto.UUIDValueDto toDto(commons.UUIDValue proto) {
        if (proto == null) return null;
        
        return new CommonDto.UUIDValueDto(proto.getId());
    }

    // DTO to Proto mappings
    public CreateProductRequest toProto(CreateProductRequestDto dto) {
        if (dto == null) return null;
        
        return CreateProductRequest.newBuilder()
            .setStoreId(dto.getStoreId() != null ? dto.getStoreId() : "")
            .setStaffId(dto.getStaffId() != null ? dto.getStaffId() : "")
            .setName(dto.getName() != null ? dto.getName() : "")
            .setImgLink(dto.getImgLink() != null ? dto.getImgLink() : "")
            .build();
    }

    public UpdateProductRequest toProto(UpdateProductRequestDto dto) {
        if (dto == null) return null;
        
        return UpdateProductRequest.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : "")
            .setStoreId(dto.getStoreId() != null ? dto.getStoreId() : "")
            .setStaffId(dto.getStaffId() != null ? dto.getStaffId() : "")
            .setTitle(dto.getTitle() != null ? dto.getTitle() : "")
            .setContent(dto.getContent() != null ? dto.getContent() : "")
            .setDescription(dto.getDescription() != null ? dto.getDescription() : "")
            .setImgLink(dto.getImgLink() != null ? dto.getImgLink() : "")
            .setTotalWaitingShip(dto.getTotalWaitingShip() != null ? dto.getTotalWaitingShip() : 0)
            .setTotalHearts(dto.getTotalHearts() != null ? dto.getTotalHearts() : 0)
            .setTotalViews(dto.getTotalViews() != null ? dto.getTotalViews() : 0)
            .setLangCode(dto.getLangCode() != null ? dto.getLangCode() : "")
            .setEnable(dto.getEnable() != null ? dto.getEnable() : false)
            .build();
    }

    public commons.UUIDValue toUUIDValueProto(String id) {
        return commons.UUIDValue.newBuilder()
            .setId(id != null ? id : "")
            .build();
    }

    public commons.PageRequest toProto(CommonDto.PageRequestDto dto) {
        if (dto == null) return null;
        
        return commons.PageRequest.newBuilder()
            .setPage(dto.getPage() != null ? dto.getPage() : 0)
            .setSize(dto.getSize() != null ? dto.getSize() : 10)
            .build();
    }

    // Helper methods
    private Instant toInstant(Timestamp timestamp) {
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
