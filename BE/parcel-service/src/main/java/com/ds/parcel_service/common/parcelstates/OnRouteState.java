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
                yield ParcelStatus.DELAYED;
            case CAN_NOT_DELIVERY:
            case CUSTOMER_REJECT:
                yield ParcelStatus.FAILED;
            default:
                throw new IllegalStateException("Invalid event at " + getParcelStatus().name() + ": " + event.name());
        };
    }

    @Override
    public ParcelStatus getParcelStatus() {
        return ParcelStatus.ON_ROUTE;
    }

}
