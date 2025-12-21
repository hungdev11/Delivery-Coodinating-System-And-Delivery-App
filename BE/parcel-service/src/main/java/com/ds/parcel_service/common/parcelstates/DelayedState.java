package com.ds.parcel_service.common.parcelstates;

import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;

public class DelayedState implements IParcelState{

    @Override
    public ParcelStatus handleTransition(ParcelEvent event) {
        return switch(event) {
            case END_SESSION:
                yield ParcelStatus.IN_WAREHOUSE;
            case CUSTOMER_CONFIRM_NOT_RECEIVED:
                // Allow admin to set dispute from any status (client may contact admin directly)
                yield ParcelStatus.DISPUTE;
            default:
                throw new IllegalStateException("Invalid event at " + getParcelStatus().name() + ": " + event.name());
        };
    }

    @Override
    public ParcelStatus getParcelStatus() {
        return ParcelStatus.DELAYED;
    }

}
