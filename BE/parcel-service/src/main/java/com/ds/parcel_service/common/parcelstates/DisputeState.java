package com.ds.parcel_service.common.parcelstates;

import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;

public class DisputeState implements IParcelState{

    @Override
    public ParcelStatus handleTransition(ParcelEvent event) {
        return switch(event) {
            case CUSTOMER_RETRACT_DISPUTE:
                yield ParcelStatus.SUCCEEDED;
            case MISSUNDERSTANDING_DISPUTE:
                yield ParcelStatus.SUCCEEDED;
            case FAULT_DISPUTE:
                yield ParcelStatus.LOST;
            default:
                throw new IllegalStateException("Invalid event at " + getParcelStatus().name() + ": " + event.name());
        };
    }

    @Override
    public ParcelStatus getParcelStatus() {
        return ParcelStatus.DISPUTE;
    }
}
