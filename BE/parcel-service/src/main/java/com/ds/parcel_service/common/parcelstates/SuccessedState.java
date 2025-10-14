package com.ds.parcel_service.common.parcelstates;

import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;

public class SuccessedState implements IParcelState{

    @Override
    public ParcelStatus handleTransition(ParcelEvent event) {
        throw new IllegalStateException("Can not transition to any state from final state :" + getParcelStatus() );
    }

    @Override
    public ParcelStatus getParcelStatus() {
        return ParcelStatus.SUCCESSED;
    }

}
