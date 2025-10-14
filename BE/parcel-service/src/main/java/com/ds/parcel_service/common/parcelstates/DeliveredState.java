package com.ds.parcel_service.common.parcelstates;

import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;

public class DeliveredState implements IParcelState{

    @Override
    public ParcelStatus handleTransition(ParcelEvent event) {
        return switch(event) {
            case CONFIRM_REMINDER:
                yield ParcelStatus.DELIVERED;
            case CONFIRM_TIMEOUT, CUSTOMER_RECEIVED:
                yield ParcelStatus.SUCCESSED;
            case CUSTOMER_REJECT:
                yield ParcelStatus.FAILED;
            default:
                throw new IllegalArgumentException("Invalid event at " + getParcelStatus().name() + ": " + event.name());
        };
    }

    @Override
    public ParcelStatus getParcelStatus() {
        return ParcelStatus.DELIVERED;
    }

}
