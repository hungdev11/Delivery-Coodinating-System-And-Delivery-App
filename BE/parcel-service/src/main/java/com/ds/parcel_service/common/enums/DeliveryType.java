package com.ds.parcel_service.common.enums;

public enum DeliveryType {
    URGENT(10), EXPRESS(4), FAST(3), NORMAL(2), ECONOMY(1);

    int priority;
    private DeliveryType(int priority) {
        this.priority = priority;
    }
}
