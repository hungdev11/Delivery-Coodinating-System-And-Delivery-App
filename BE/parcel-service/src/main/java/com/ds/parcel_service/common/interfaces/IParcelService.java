package com.ds.parcel_service.common.interfaces;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ds.parcel_service.common.entities.dto.request.ParcelCreateRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelFilterRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelUpdateRequest;
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
    PageResponse<ParcelResponse> getParcels(
        ParcelFilterRequest filter, 
        int page, 
        int size, 
        String sortBy,
        String direction
    );
    Map<String, ParcelResponse> fetchParcelsBulk(List<UUID> parcelIds);
}
