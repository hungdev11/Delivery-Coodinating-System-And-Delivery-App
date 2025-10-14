package com.ds.parcel_service.common.parcelstates;

import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;

public interface IParcelState {
    ParcelStatus handleTransition(ParcelEvent event);
    ParcelStatus getParcelStatus();
}
