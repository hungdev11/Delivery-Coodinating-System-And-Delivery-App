package com.ds.parcel_service.business.v1.services;


import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ds.parcel_service.app_context.models.Parcel;
import com.ds.parcel_service.app_context.repositories.ParcelRepository;
import com.ds.parcel_service.common.entities.dto.request.ParcelCreateRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelUpdateRequest;
import com.ds.parcel_service.common.entities.dto.response.PageResponse;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.enums.DeliveryType;
import com.ds.parcel_service.common.enums.ParcelStatus;
import com.ds.parcel_service.common.exceptions.ResourceNotFound;
import com.ds.parcel_service.common.interfaces.IParcelService;
import com.ds.parcel_service.common.utils.PageUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParcelService implements IParcelService{

    private final ParcelRepository parcelRepository;

    @Override
    public ParcelResponse createParcel(ParcelCreateRequest request) {
        /* 
         * tạo parcel, lấy address trong parcel tạo destination
        */
        validateUniqueCode(request.getCode());
        Parcel parcel = Parcel.builder()
                            .code(request.getCode())
                            .deliveryType(DeliveryType.valueOf(request.getDeliveryType()))
                            .senderId(request.getSenderId())
                            .receiverId(request.getReceiverId())
                            .receiveFrom(request.getReceiveFrom())
                            .sendTo(request.getSendTo())
                            .status(ParcelStatus.IN_WAREHOUSE)
                            .weight(request.getWeight())
                            .value(request.getValue())
                            .build();

        // Call zone service create or get destination
        // create parcel destination properly
        // get receiver info (phone number)

        Parcel savedParcel = parcelRepository.save(parcel);
        return toDto(savedParcel);
    }

    @Override
    public ParcelResponse updateParcel(UUID parcelId, ParcelUpdateRequest request) {
        Parcel parcel = parcelRepository.findById(parcelId).orElseThrow(() -> {
            return new ResourceNotFound("Parcel not found");
        });
        parcel.setWeight(request.getWeight());
        parcel.setValue(request.getValue());
        return toDto(parcelRepository.save(parcel));
    }

    @Override
    public void deleteParcel(UUID parcelId) {
        //haven't seen usecase to use
        throw new UnsupportedOperationException("Unimplemented method 'deleteParcel'");
    }

    @Override
    public ParcelResponse getParcelById(UUID parcelId) {
        return toDto(getParcel(parcelId));    
    }

    @Override
    public ParcelResponse getParcelByCode(String code) {
        return toDto(parcelRepository.findByCode(code).orElseThrow(() -> new RuntimeException("Parcel with code already exists")));
    }

    @Override
    public PageResponse<ParcelResponse> getParcels(int page, int size, String sortBy,
            String direction) {
        Pageable pageable = PageUtil.build(page, size, sortBy, direction, Parcel.class);
        Page<Parcel> parcels = parcelRepository.findAll(pageable);
        return PageResponse.from(parcels.map(this::toDto));
    }

    private ParcelResponse toDto(Parcel parcel) {
        //get phone number here
        return ParcelResponse.builder()
                            .code(parcel.getCode())
                            .deliveryType(parcel.getDeliveryType())
                            .senderId(parcel.getSenderId())
                            .receiverId(parcel.getReceiverId())
                            .receiveFrom(parcel.getReceiveFrom())
                            .targetDestination(parcel.getSendTo())
                            .weight(parcel.getWeight())
                            .value(parcel.getValue())
                            .status(parcel.getStatus())
                            .createdAt(parcel.getCreatedAt())
                            .updatedAt(parcel.getUpdatedAt())
                            .build();
    }

    private void validateUniqueCode(String code) {
        if (parcelRepository.existsByCode(code)) {
            throw new RuntimeException("Parcel with code already exists");
        }
    }

    private Parcel getParcel(UUID id) {
        return parcelRepository.findById(id).orElseThrow(()->{
            return new ResourceNotFound("Parcel not found");
        });
    }

    //update status
    //update address
    // parcel staticstic
}
