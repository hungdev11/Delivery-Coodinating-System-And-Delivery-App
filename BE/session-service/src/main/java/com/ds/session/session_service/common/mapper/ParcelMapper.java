package com.ds.session.session_service.common.mapper;

import org.mapstruct.Mapper;

import com.ds.session.session_service.application.client.parcelclient.response.ParcelResponse;
import com.ds.session.session_service.business.v1.services.ParcelInfo;

@Mapper(componentModel = "spring") 
public interface ParcelMapper {

    ParcelInfo toParcelInfo(ParcelResponse response);
    
}
