package ptithcm.graduation.apigateway.services.baseService.product.mapper;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import attributes.*;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AttributeMapper {

    // Proto to DTO mappings
    public AttributeDto toDto(Attribute proto) {
        if (proto == null) return null;
        
        List<AttributeDto> attributeValueList = proto.getAttributeValueListList().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        
        return new AttributeDto(
            proto.getAttributeId(),
            proto.getName(),
            proto.getDescription(),
            attributeValueList
        );
    }

    public List<AttributeDto> toDto(AttributeList proto) {
        if (proto == null) return null;
        
        return proto.getContentList().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    // DTO to Proto mappings
    public CreateAttributeRequest toCreateProto(AttributeDto dto, String parentId) {
        if (dto == null) return null;
        
        CreateAttributeRequest.Builder builder = CreateAttributeRequest.newBuilder()
            .setName(dto.getName() != null ? dto.getName() : "")
            .setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        
        if (parentId != null) {
            builder.setParentId(parentId);
        }
        
        return builder.build();
    }

    public UpdateAttributeRequest toUpdateProto(AttributeDto dto) {
        if (dto == null) return null;
        
        return UpdateAttributeRequest.newBuilder()
            .setId(dto.getAttributeId() != null ? dto.getAttributeId() : 0L)
            .setName(dto.getName() != null ? dto.getName() : "")
            .setDescription(dto.getDescription() != null ? dto.getDescription() : "")
            .build();
    }

    public AttributeId toIdProto(Long attributeId) {
        return AttributeId.newBuilder()
            .setAttributeId(attributeId != null ? attributeId : 0L)
            .build();
    }
}
