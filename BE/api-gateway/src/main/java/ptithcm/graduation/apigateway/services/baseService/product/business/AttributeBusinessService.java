package ptithcm.graduation.apigateway.services.baseService.product.business;

import ptithcm.graduation.apigateway.services.baseService.product.dto.*;
import ptithcm.graduation.apigateway.services.baseService.product.interfaces.IAttributeService;
import ptithcm.graduation.apigateway.services.baseService.product.mapper.AttributeMapper;
import attributes.*;
import commons.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.devh.boot.grpc.client.inject.GrpcClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AttributeBusinessService implements IAttributeService {

    @GrpcClient("attribute-service")
    private AttributeServiceGrpc.AttributeServiceFutureStub attributeServiceStub;

    @Autowired
    private AttributeMapper attributeMapper;

    @Override
    public CompletableFuture<List<AttributeDto>> getAttributeListByProductOptionId(String productOptionId) {
        UUIDValue request = UUIDValue.newBuilder().setId(productOptionId).build();
        return CompletableFuture.supplyAsync(() -> {
            try {
                AttributeList response = attributeServiceStub.getAttributeListByProductOptionId(request).get();
                return attributeMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error getting attributes by product option id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<AttributeDto> getAttributeById(Long attributeId) {
        AttributeId request = attributeMapper.toIdProto(attributeId);
        return CompletableFuture.supplyAsync(() -> {
            try {
                Attribute response = attributeServiceStub.getAttributeById(request).get();
                return attributeMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error getting attribute by id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<AttributeDto> createAttribute(AttributeDto request, String parentId) {
        CreateAttributeRequest grpcRequest = attributeMapper.toCreateProto(request, parentId);
        return CompletableFuture.supplyAsync(() -> {
            try {
                Attribute response = attributeServiceStub.createAttribute(grpcRequest).get();
                return attributeMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error creating attribute: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<AttributeDto> updateAttribute(AttributeDto request) {
        UpdateAttributeRequest grpcRequest = attributeMapper.toUpdateProto(request);
        return CompletableFuture.supplyAsync(() -> {
            try {
                Attribute response = attributeServiceStub.updateAttribute(grpcRequest).get();
                return attributeMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error updating attribute: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteAttributeById(Long attributeId) {
        AttributeId request = attributeMapper.toIdProto(attributeId);
        return CompletableFuture.supplyAsync(() -> {
            try {
                attributeServiceStub.deleteAttributeByID(request).get();
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Error deleting attribute by id: " + e.getMessage(), e);
            }
        });
    }
}
