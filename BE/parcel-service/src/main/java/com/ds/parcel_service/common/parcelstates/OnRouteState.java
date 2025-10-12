package com.ds.parcel_service.common.parcelstates;

import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;

public class OnRouteState implements IParcelState{

    @Override
    public ParcelStatus handleTransition(ParcelEvent event) {
        return switch(event) {
            case DELIVERY_SUCCESSFUL:
                yield ParcelStatus.DELIVERED;
            case POSTPONE:
                yield ParcelStatus.IN_WAREHOUSE;
            case CAN_NOT_DELIVERY, ACCIDENT:
                yield ParcelStatus.FAILED;
            default:
                throw new IllegalArgumentException("Invalid event at " + getParcelStatus().name() + ": " + event.name());
        };
    }

    @Override
    public ParcelStatus getParcelStatus() {
        return ParcelStatus.ON_ROUTE;
    }

}
