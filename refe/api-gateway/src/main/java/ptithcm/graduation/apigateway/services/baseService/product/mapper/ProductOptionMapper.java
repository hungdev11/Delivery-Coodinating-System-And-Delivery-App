package ptithcm.graduation.apigateway.services.baseService.product.mapper;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import products.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductOptionMapper {

    @Autowired
    private ProductMapper productMapper;

    // Proto to DTO mappings
    public ProductOptionDto toDto(ProductOption proto) {
        if (proto == null) return null;
        
        ProductOptionDto dto = new ProductOptionDto();
        dto.setId(proto.getId());
        dto.setSku(proto.getSku());
        dto.setName(proto.getName());
        dto.setPriceRetail(proto.getPriceRetail());
        dto.setPriceSale(proto.getPriceSale());
        dto.setTotalWaitingShip(proto.getTotalWaitingShip());
        dto.setEnable(proto.getEnable());
        
        // Handle oneof productRef
        switch (proto.getProductRefCase()) {
            case PRODUCTID:
                dto.setProductId(proto.getProductId());
                break;
            case PRODUCT:
                dto.setProduct(productMapper.toDto(proto.getProduct()));
                break;
            default:
                break;
        }
        
        return dto;
    }

    // DTO to Proto mappings
    public CreateProductOptionRequest toCreateProto(ProductOptionDto dto) {
        if (dto == null) return null;
        
        CreateProductOptionRequest.Builder builder = CreateProductOptionRequest.newBuilder()
            .setSku(dto.getSku() != null ? dto.getSku() : "")
            .setName(dto.getName() != null ? dto.getName() : "")
            .setPriceRetail(dto.getPriceRetail() != null ? dto.getPriceRetail() : 0.0)
            .setPriceSale(dto.getPriceSale() != null ? dto.getPriceSale() : 0.0);
        
        if (dto.getProductId() != null) {
            builder.setProductId(dto.getProductId());
        } else if (dto.getProduct() != null) {
            // Note: Would need to convert ProductDto back to Product proto
            // This might require additional logic based on business requirements
        }
        
        return builder.build();
    }

    public UpdateProductOptionRequest toUpdateProto(ProductOptionDto dto) {
        if (dto == null) return null;
        
        UpdateProductOptionRequest.Builder builder = UpdateProductOptionRequest.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : 0L)
            .setSku(dto.getSku() != null ? dto.getSku() : "")
            .setName(dto.getName() != null ? dto.getName() : "")
            .setPriceRetail(dto.getPriceRetail() != null ? dto.getPriceRetail() : 0.0)
            .setPriceSale(dto.getPriceSale() != null ? dto.getPriceSale() : 0.0)
            .setTotalWaitingShip(dto.getTotalWaitingShip() != null ? dto.getTotalWaitingShip() : 0)
            .setEnable(dto.getEnable() != null ? dto.getEnable() : false);
        
        if (dto.getProductId() != null) {
            builder.setProductId(dto.getProductId());
        } else if (dto.getProduct() != null) {
            // Note: Would need to convert ProductDto back to Product proto
        }
        
        return builder.build();
    }

    public ProductOptionId toIdProto(Long id) {
        return ProductOptionId.newBuilder()
            .setId(id != null ? id : 0L)
            .build();
    }
}
