package com.ds.parcel_service.common.parcelstates;

import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;

public class InWarehouseState implements IParcelState{

    @Override
    public ParcelStatus handleTransition(ParcelEvent event) {
        return switch(event) {
            case SCAN_QR:
                yield ParcelStatus.ON_ROUTE;
            default:
                throw new IllegalArgumentException("Invalid event at " + getParcelStatus().name() + ": " + event.name());
        };
    }

    @Override
    public ParcelStatus getParcelStatus() {
        return ParcelStatus.IN_WAREHOUSE;
    }

}
