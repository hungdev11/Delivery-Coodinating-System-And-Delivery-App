package com.ds.user.common.interfaces;

import com.ds.user.common.entities.common.paging.PagedData;
import com.ds.user.common.entities.common.PagingRequest;
import com.ds.user.common.entities.common.PagingRequestV2;
import com.ds.user.common.entities.dto.deliveryman.CreateDeliveryManRequest;
import com.ds.user.common.entities.dto.deliveryman.DeliveryManDto;
import com.ds.user.common.entities.dto.deliveryman.UpdateDeliveryManRequest;

import java.util.Optional;
import java.util.UUID;

public interface IDeliveryManService {
    DeliveryManDto createDeliveryMan(CreateDeliveryManRequest request);
    DeliveryManDto updateDeliveryMan(UUID id, UpdateDeliveryManRequest request);
    void deleteDeliveryMan(UUID id);
    Optional<DeliveryManDto> getDeliveryMan(UUID id);
    Optional<DeliveryManDto> getDeliveryManByUserId(String userId); // Changed from UUID to String
    PagedData<DeliveryManDto> getDeliveryMans(PagingRequest query);
    PagedData<DeliveryManDto> getDeliveryMansV2(PagingRequestV2 query);
}
