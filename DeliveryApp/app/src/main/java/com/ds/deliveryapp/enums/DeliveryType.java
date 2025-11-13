package com.ds.deliveryapp.enums;

public enum DeliveryType {
    URGENT(0), EXPRESS(1), FAST(2), NORMAL(3), ECONOMY(4);

    int priority;
    private DeliveryType(int priority) {
        this.priority = priority;
    }
}

