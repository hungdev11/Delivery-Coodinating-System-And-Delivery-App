package ptithcm.graduation.apigateway.services.baseService.product.interfaces;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IAttributeService {
    
    // Attribute CRUD operations
    CompletableFuture<List<AttributeDto>> getAttributeListByProductOptionId(String productOptionId);
    CompletableFuture<AttributeDto> getAttributeById(Long attributeId);
    CompletableFuture<AttributeDto> createAttribute(AttributeDto request, String parentId);
    CompletableFuture<AttributeDto> updateAttribute(AttributeDto request);
    CompletableFuture<Void> deleteAttributeById(Long attributeId);
}
