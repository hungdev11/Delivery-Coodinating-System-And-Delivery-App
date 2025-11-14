package com.ds.parcel_service.common.interfaces;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ds.parcel_service.common.entities.dto.request.ParcelCreateRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelFilterRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelUpdateRequest;
import com.ds.parcel_service.common.entities.dto.request.PagingRequestV0;
import com.ds.parcel_service.common.entities.dto.request.PagingRequestV2;
import com.ds.parcel_service.common.entities.dto.response.PageResponse;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.enums.ParcelEvent;

public interface IParcelService {
    ParcelResponse createParcel(ParcelCreateRequest request);
    ParcelResponse updateParcel(UUID parcelId, ParcelUpdateRequest request);
    void deleteParcel(UUID parcelId);
    ParcelResponse changeParcelStatus(UUID parcelId, ParcelEvent event);
    ParcelResponse getParcelById(UUID parcelId);
    ParcelResponse getParcelByCode(String code);
    
    // V1 API
    PageResponse<ParcelResponse> getParcels(
        ParcelFilterRequest filter, 
        int page, 
        int size, 
        String sortBy,
        String direction
    );
    
    // V0 API - Simple paging
    PageResponse<ParcelResponse> getParcelsV0(PagingRequestV0 request);
    
    // V2 API - Enhanced filtering
    PageResponse<ParcelResponse> getParcelsV2(PagingRequestV2 request);
    
    Map<String, ParcelResponse> fetchParcelsBulk(List<UUID> parcelIds);

    PageResponse<ParcelResponse> getParcelsSentByCustomer(String customerId, int page, int size);

    PageResponse<ParcelResponse> getParcelsReceivedByCustomer(String customerId, int page, int size);
    
    // Priority and delay management
    ParcelResponse updateParcelPriority(UUID parcelId, Integer priority);
    ParcelResponse delayParcel(UUID parcelId, LocalDateTime delayedUntil);
    ParcelResponse undelayParcel(UUID parcelId);
}
