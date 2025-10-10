package com.ds.parcel_service.common.interfaces;

import java.util.UUID;

import com.ds.parcel_service.common.entities.dto.request.ParcelCreateRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelUpdateRequest;
import com.ds.parcel_service.common.entities.dto.response.PageResponse;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;

public interface IParcelService {
    ParcelResponse createParcel(ParcelCreateRequest request);
    ParcelResponse updateParcel(UUID parcelId, ParcelUpdateRequest request);
    void deleteParcel(UUID parcelId);
    ParcelResponse getParcelById(UUID parcelId);
    ParcelResponse getParcelByCode(String code);
    PageResponse<ParcelResponse> getParcels(int page, int size, String sortBy, String direction);
}
